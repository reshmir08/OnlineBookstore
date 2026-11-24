package com.bookstore.app.data.repo

import androidx.room.withTransaction
import com.bookstore.app.core.util.AppResult
import com.bookstore.app.core.util.DeliveryEstimator
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.AddressDao
import com.bookstore.app.data.local.BookstoreDatabase
import com.bookstore.app.data.local.CartDao
import com.bookstore.app.data.local.CartLine
import com.bookstore.app.data.local.CatalogDao
import com.bookstore.app.data.local.OrderDao
import com.bookstore.app.data.local.OrderEntity
import com.bookstore.app.data.local.OrderItemEntity
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.local.OrderWithItems
import com.bookstore.app.data.payment.PaymentDetails
import com.bookstore.app.data.payment.PaymentGateway
import com.bookstore.app.data.payment.PaymentOutcome
import com.bookstore.app.domain.CancellationPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

sealed interface CheckoutResult {
    data class Success(val orderId: String) : CheckoutResult
    data class PaymentFailed(val reason: String) : CheckoutResult
    data class Error(val message: String) : CheckoutResult
}

class OrderRepository(
    private val orderDao: OrderDao,
    private val cartDao: CartDao,
    private val catalogDao: CatalogDao,
    private val session: SessionManager,
    private val gateway: PaymentGateway,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val runInTransaction: suspend (suspend () -> Any?) -> Any? = { block -> block() }
) {
    constructor(
        db: BookstoreDatabase,
        session: SessionManager,
        gateway: PaymentGateway,
        clock: Clock = Clock.systemDefaultZone()
    ) : this(
        orderDao = db.orderDao(),
        cartDao = db.cartDao(),
        catalogDao = db.catalogDao(),
        session = session,
        gateway = gateway,
        clock = clock,
        runInTransaction = { block -> db.withTransaction { block() } }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun orders(): Flow<List<OrderWithItems>> =
        session.userId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else orderDao.orders(id) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun order(orderId: String): Flow<OrderWithItems?> =
        session.userId.flatMapLatest { id -> if (id == null) flowOf(null) else orderDao.order(orderId, id) }

    fun deliveryDateFor(lines: List<CartLine>): LocalDate {
        val days = lines.maxOfOrNull { it.book.deliveryDays } ?: 0
        return DeliveryEstimator.estimate(LocalDate.now(clock), days)
    }

    /** Charges the customer, then (only on approval) creates the order, reserves stock and empties the basket. */
    suspend fun checkout(address: AddressEntity, details: PaymentDetails): CheckoutResult {
        val userId = session.userId.value ?: return CheckoutResult.Error("Please sign in first")
        val lines = cartDao.linesOnce(userId)
        if (lines.isEmpty()) return CheckoutResult.Error("Your basket is empty")

        // Validate against the latest stock before taking payment.
        lines.firstOrNull { it.book.stock < it.item.quantity }?.let {
            return CheckoutResult.Error("Only ${it.book.stock} copies of \"${it.book.title}\" are available")
        }
        val total = lines.sumOf { it.lineTotal }

        val transactionId = when (val outcome = gateway.charge(total, details)) {
            is PaymentOutcome.Declined -> return CheckoutResult.PaymentFailed(outcome.reason)
            is PaymentOutcome.Approved -> outcome.transactionId
        }

        val now = clock.millis()
        val orderId = newOrderId()
        return try {
            runInTransaction {
                for (line in lines) {
                    check(catalogDao.decrementStock(line.book.id, line.item.quantity) == 1) {
                        "\"${line.book.title}\" just went out of stock"
                    }
                }
                orderDao.insertOrder(
                    OrderEntity(
                        id = orderId,
                        userId = userId,
                        status = OrderStatus.PLACED,
                        placedAt = now,
                        cancelledAt = null,
                        total = total,
                        estimatedDeliveryEpochDay = deliveryDateFor(lines).toEpochDay(),
                        addressName = address.fullName,
                        addressPhone = address.phone,
                        addressText = address.formatted(),
                        paymentMethod = details.method,
                        paymentDetail = details.summary(),
                        transactionId = transactionId,
                        paidAt = now
                    )
                )
                orderDao.insertItems(lines.map {
                    OrderItemEntity(
                        orderId = orderId, bookId = it.book.id, title = it.book.title, author = it.book.author,
                        brand = it.book.brand, unitPrice = it.book.price, quantity = it.item.quantity,
                        coverStart = it.book.coverStart, coverEnd = it.book.coverEnd
                    )
                })
                cartDao.clear(userId)
                CheckoutResult.Success(orderId)
            } as CheckoutResult
        } catch (e: IllegalStateException) {
            CheckoutResult.Error("${e.message}. Your payment ($transactionId) will be refunded.")
        }
    }

    /** Cancels an order if it is still inside the 48-hour window, measured from its placement timestamp. */
    @Suppress("UNCHECKED_CAST")
    suspend fun cancel(orderId: String): AppResult<Unit> {
        val userId = session.userId.value ?: return AppResult.Failure("Please sign in first")
        return runInTransaction {
            val found = orderDao.orderOnce(orderId, userId) ?: return@runInTransaction AppResult.Failure("Order not found")
            val now = clock.millis()
            when {
                found.order.status == OrderStatus.CANCELLED ->
                    AppResult.Failure("This order has already been cancelled")
                !CancellationPolicy.isEligible(found.order, now) ->
                    AppResult.Failure("Orders can only be cancelled within 48 hours of being placed")
                else -> {
                    orderDao.updateStatus(orderId, OrderStatus.CANCELLED, now)
                    found.items.forEach { catalogDao.incrementStock(it.bookId, it.quantity) }
                    AppResult.Success(Unit)
                }
            }
        } as AppResult<Unit>
    }

    fun nowMillis(): Long = clock.millis()

    private fun newOrderId(): String {
        val date = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE)
        return "PT$date-${Random.nextInt(100_000, 999_999)}"
    }
}
