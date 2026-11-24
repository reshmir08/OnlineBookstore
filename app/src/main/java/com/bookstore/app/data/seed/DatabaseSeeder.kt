package com.bookstore.app.data.seed

import com.bookstore.app.core.util.DeliveryEstimator
import com.bookstore.app.core.util.PasswordHasher
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.BookstoreDatabase
import com.bookstore.app.data.local.OrderEntity
import com.bookstore.app.data.local.OrderItemEntity
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.local.PaymentMethod
import com.bookstore.app.data.local.UserEntity
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Populates an empty database with the catalogue plus a demo customer who already has order history. */
object DatabaseSeeder {
    const val DEMO_EMAIL = "demo@pageturner.com"
    const val DEMO_PASSWORD = "Books1234"

    suspend fun seedIfEmpty(db: BookstoreDatabase, nowMillis: Long = System.currentTimeMillis()) {
        val catalog = db.catalogDao()
        if (catalog.categoryCount() == 0) {
            catalog.insertCategories(SeedData.categories)
            catalog.insertBooks(SeedData.books)
        }
        val userDao = db.userDao()
        val existing = userDao.findByEmail(DEMO_EMAIL)
        if (existing == null) {
            seedDemoCustomer(db, nowMillis)
        } else if (!PasswordHasher.verify(DEMO_PASSWORD, existing.salt, existing.passwordHash)) {
            val salt = PasswordHasher.newSalt()
            val updated = existing.copy(
                passwordHash = PasswordHasher.hash(DEMO_PASSWORD, salt),
                salt = salt
            )
            userDao.update(updated)
        }
    }

    private suspend fun seedDemoCustomer(db: BookstoreDatabase, now: Long) {
        val salt = PasswordHasher.newSalt()
        val userId = db.userDao().insert(
            UserEntity(
                name = "Demo Reader", email = DEMO_EMAIL,
                passwordHash = PasswordHasher.hash(DEMO_PASSWORD, salt), salt = salt, createdAt = now
            )
        )
        val home = AddressEntity(
            userId = userId, label = "Home", fullName = "Demo Reader", phone = "9876543210",
            line1 = "12, Lake View Apartments", line2 = "MG Road", city = "Bengaluru",
            state = "Karnataka", postalCode = "560001", isDefault = true
        )
        val office = AddressEntity(
            userId = userId, label = "Office", fullName = "Demo Reader", phone = "9876543210",
            line1 = "4th Floor, Tech Park", line2 = "Outer Ring Road", city = "Bengaluru",
            state = "Karnataka", postalCode = "560103", isDefault = false
        )
        db.addressDao().insert(home)
        db.addressDao().insert(office)

        // A recent order (inside the 48h cancellation window) and an older one (outside it).
        seedOrder(db, userId, home, "PT20240101-100001", now - TimeUnit.HOURS.toMillis(5), listOf(601L to 1, 401L to 1), PaymentMethod.UPI, "UPI demo@upi")
        seedOrder(db, userId, home, "PT20231201-100002", now - TimeUnit.DAYS.toMillis(20), listOf(103L to 1, 201L to 1, 301L to 1), PaymentMethod.CARD, "Card ending 4242")
    }

    private suspend fun seedOrder(
        db: BookstoreDatabase, userId: Long, address: AddressEntity, id: String, placedAt: Long,
        lines: List<Pair<Long, Int>>, method: PaymentMethod, paymentDetail: String
    ) {
        val books = lines.map { (bookId, qty) -> requireNotNull(db.catalogDao().bookOnce(bookId)) to qty }
        val placedDate = Instant.ofEpochMilli(placedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val delivery = DeliveryEstimator.estimate(placedDate, books.maxOf { it.first.deliveryDays })
        db.orderDao().insertOrder(
            OrderEntity(
                id = id, userId = userId, status = OrderStatus.PLACED, placedAt = placedAt, cancelledAt = null,
                total = books.sumOf { (b, q) -> b.price * q },
                estimatedDeliveryEpochDay = delivery.toEpochDay(),
                addressName = address.fullName, addressPhone = address.phone, addressText = address.formatted(),
                paymentMethod = method, paymentDetail = paymentDetail,
                transactionId = "TXN" + id.takeLast(6) + "SEED", paidAt = placedAt
            )
        )
        db.orderDao().insertItems(books.map { (b, q) ->
            OrderItemEntity(
                orderId = id, bookId = b.id, title = b.title, author = b.author, brand = b.brand,
                unitPrice = b.price, quantity = q, coverStart = b.coverStart, coverEnd = b.coverEnd
            )
        })
    }
}
