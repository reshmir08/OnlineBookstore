package com.bookstore.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class OrderStatus { PLACED, CANCELLED }

enum class PaymentMethod(val label: String) {
    CARD("Credit / Debit card"),
    UPI("UPI"),
    NET_BANKING("Net banking")
}

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String,
    val iconKey: String
)

/** [brand] is the publisher. Colours drive the generated cover artwork. */
@Entity(tableName = "books", indices = [Index("categoryId"), Index("brand")])
data class BookEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val author: String,
    val brand: String,
    val categoryId: Long,
    val description: String,
    val price: Long,
    val stock: Int,
    val deliveryDays: Int,
    val coverStart: Long,
    val coverEnd: Long,
    val popularity: Int
)

@Entity(tableName = "cart_items", primaryKeys = ["userId", "bookId"])
data class CartItemEntity(
    val userId: Long,
    val bookId: Long,
    val quantity: Int
)

@Entity(tableName = "addresses", indices = [Index("userId")])
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val label: String,
    val fullName: String,
    val phone: String,
    val line1: String,
    val line2: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val isDefault: Boolean
) {
    fun formatted(): String = buildString {
        append(line1)
        if (line2.isNotBlank()) append(", ").append(line2)
        append("\n").append(city).append(", ").append(state).append(" - ").append(postalCode)
    }
}

@Entity(tableName = "orders", indices = [Index("userId")])
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: Long,
    val status: OrderStatus,
    /** Order placement timestamp (epoch millis). The 48-hour cancellation window is measured from this. */
    val placedAt: Long,
    val cancelledAt: Long?,
    val total: Long,
    val estimatedDeliveryEpochDay: Long,
    val addressName: String,
    val addressPhone: String,
    val addressText: String,
    val paymentMethod: PaymentMethod,
    val paymentDetail: String,
    val transactionId: String,
    val paidAt: Long
)

/** Snapshot of a purchased book, so order history stays accurate if the catalogue changes. */
@Entity(tableName = "order_items", indices = [Index("orderId")])
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,
    val bookId: Long,
    val title: String,
    val author: String,
    val brand: String,
    val unitPrice: Long,
    val quantity: Int,
    val coverStart: Long,
    val coverEnd: Long
)

@Entity(tableName = "browse_events", indices = [Index("userId")])
data class BrowseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val viewedAt: Long
)

data class CartLine(
    @Embedded val item: CartItemEntity,
    @Relation(parentColumn = "bookId", entityColumn = "id") val book: BookEntity
) {
    val lineTotal: Long get() = book.price * item.quantity
}

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(parentColumn = "id", entityColumn = "orderId") val items: List<OrderItemEntity>
)
