package com.bookstore.app.data.repo

import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.payment.PaymentDetails
import com.bookstore.app.data.payment.SimulatedPaymentGateway
import com.bookstore.app.testutil.FakeCartDao
import com.bookstore.app.testutil.FakeCatalogDao
import com.bookstore.app.testutil.FakeOrderDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class OrderRepositoryTest {
    private lateinit var orderDao: FakeOrderDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var session: SessionManager
    private lateinit var gateway: SimulatedPaymentGateway
    private lateinit var orderRepo: OrderRepository

    private val fixedInstant = Instant.parse("2025-01-01T10:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val book1 = BookEntity(1, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 90)
    private val book2 = BookEntity(2, "Atomic Habits", "James Clear", "Penguin", 2, "Desc", 400, 5, 2, 0L, 0L, 95)

    private val address = AddressEntity(
        id = 1, userId = 10, label = "Home", fullName = "Test Customer", phone = "9876543210",
        line1 = "123 Main St", line2 = "", city = "Bengaluru", state = "Karnataka",
        postalCode = "560001", isDefault = true
    )

    private val validCard = PaymentDetails.Card("4242 4242 4242 4242", "Test Customer", "12/30", "123")
    private val declinedCard = PaymentDetails.Card("4000 0000 0000 0002", "Test Customer", "12/30", "123")

    @Before
    fun setUp() {
        catalogDao = FakeCatalogDao(initialBooks = listOf(book1, book2))
        cartDao = FakeCartDao(catalogDao)
        orderDao = FakeOrderDao()
        session = SessionManager(initialUserId = 10L)
        gateway = SimulatedPaymentGateway(latencyMillis = 0)
        orderRepo = OrderRepository(
            orderDao = orderDao,
            cartDao = cartDao,
            catalogDao = catalogDao,
            session = session,
            gateway = gateway,
            clock = clock
        )
    }

    @Test
    fun checkout_signedOut_returnsError() = runTest {
        session.signOut()
        val result = orderRepo.checkout(address, validCard)
        assertTrue(result is CheckoutResult.Error)
        assertEquals("Please sign in first", (result as CheckoutResult.Error).message)
    }

    @Test
    fun checkout_emptyBasket_returnsError() = runTest {
        val result = orderRepo.checkout(address, validCard)
        assertTrue(result is CheckoutResult.Error)
        assertEquals("Your basket is empty", (result as CheckoutResult.Error).message)
    }

    @Test
    fun checkout_exceedingStock_returnsError() = runTest {
        // Book 2 has stock 5, put quantity 6 directly in cart
        cartDao.upsert(CartItemEntity(10L, 2L, 6))
        val result = orderRepo.checkout(address, validCard)
        assertTrue(result is CheckoutResult.Error)
        assertTrue((result as CheckoutResult.Error).message.contains("Only 5 copies"))
    }

    @Test
    fun checkout_paymentDeclined_keepsCartAndStock() = runTest {
        cartDao.upsert(CartItemEntity(10L, 1L, 2))
        val result = orderRepo.checkout(address, declinedCard)
        assertTrue(result is CheckoutResult.PaymentFailed)

        // Cart is untouched
        assertEquals(1, cartDao.linesOnce(10L).size)
        // Stock is untouched
        assertEquals(10, catalogDao.bookOnce(1L)?.stock)
    }

    @Test
    fun checkout_paymentApproved_createsOrderDeductsStockAndEmptiesCart() = runTest {
        cartDao.upsert(CartItemEntity(10L, 1L, 2))
        cartDao.upsert(CartItemEntity(10L, 2L, 1))

        val result = orderRepo.checkout(address, validCard)
        assertTrue(result is CheckoutResult.Success)
        val orderId = (result as CheckoutResult.Success).orderId

        // Cart is cleared
        assertTrue(cartDao.linesOnce(10L).isEmpty())
        // Stock decremented: book1 was 10, now 8; book2 was 5, now 4
        assertEquals(8, catalogDao.bookOnce(1L)?.stock)
        assertEquals(4, catalogDao.bookOnce(2L)?.stock)

        // Order is saved in DAO
        val saved = orderDao.orderOnce(orderId, 10L)
        assertNotNull(saved)
        assertEquals(orderId, saved!!.order.id)
        assertEquals(1400L, saved.order.total) // (500*2) + (400*1)
        assertEquals(2, saved.items.size)
        assertEquals(OrderStatus.PLACED, saved.order.status)
    }

    @Test
    fun cancel_within48Hours_succeedsAndRestoresStock() = runTest {
        cartDao.upsert(CartItemEntity(10L, 1L, 3))
        val checkoutRes = orderRepo.checkout(address, validCard) as CheckoutResult.Success
        val orderId = checkoutRes.orderId

        assertEquals(7, catalogDao.bookOnce(1L)?.stock)

        // Advance 10 hours from placement
        val laterClock = Clock.offset(clock, java.time.Duration.ofHours(10))
        val repoWithLaterClock = OrderRepository(
            orderDao = orderDao, cartDao = cartDao, catalogDao = catalogDao,
            session = session, gateway = gateway, clock = laterClock
        )

        val cancelResult = repoWithLaterClock.cancel(orderId)
        assertTrue(cancelResult is AppResult.Success)

        // Stock restored from 7 back to 10
        assertEquals(10, catalogDao.bookOnce(1L)?.stock)

        val updatedOrder = orderDao.orderOnce(orderId, 10L)
        assertEquals(OrderStatus.CANCELLED, updatedOrder?.order?.status)
    }

    @Test
    fun cancel_after48Hours_fails() = runTest {
        cartDao.upsert(CartItemEntity(10L, 1L, 2))
        val checkoutRes = orderRepo.checkout(address, validCard) as CheckoutResult.Success
        val orderId = checkoutRes.orderId

        // Advance 50 hours
        val lateClock = Clock.offset(clock, java.time.Duration.ofHours(50))
        val repoWithLateClock = OrderRepository(
            orderDao = orderDao, cartDao = cartDao, catalogDao = catalogDao,
            session = session, gateway = gateway, clock = lateClock
        )

        val cancelResult = repoWithLateClock.cancel(orderId)
        assertTrue(cancelResult is AppResult.Failure)
        assertTrue((cancelResult as AppResult.Failure).message.contains("48 hours"))

        // Status is still PLACED
        val order = orderDao.orderOnce(orderId, 10L)
        assertEquals(OrderStatus.PLACED, order?.order?.status)
    }

    @Test
    fun cancel_alreadyCancelled_fails() = runTest {
        cartDao.upsert(CartItemEntity(10L, 1L, 1))
        val checkoutRes = orderRepo.checkout(address, validCard) as CheckoutResult.Success
        val orderId = checkoutRes.orderId

        orderRepo.cancel(orderId)

        val secondCancel = orderRepo.cancel(orderId)
        assertTrue(secondCancel is AppResult.Failure)
        assertEquals("This order has already been cancelled", (secondCancel as AppResult.Failure).message)
    }
}
