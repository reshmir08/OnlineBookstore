package com.bookstore.app.ui.orders

import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.payment.PaymentDetails
import com.bookstore.app.data.payment.SimulatedPaymentGateway
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.CheckoutResult
import com.bookstore.app.data.repo.OrderRepository
import com.bookstore.app.testutil.FakeCartDao
import com.bookstore.app.testutil.FakeCatalogDao
import com.bookstore.app.testutil.FakeOrderDao
import com.bookstore.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var orderDao: FakeOrderDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var session: SessionManager
    private lateinit var cartRepo: CartRepository
    private lateinit var orderRepo: OrderRepository

    private val fixedClock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"))
    private var orderId: String = ""

    @Before
    fun setUp() = runTest {
        session = SessionManager(initialUserId = 1L)
        catalogDao = FakeCatalogDao(
            initialBooks = listOf(
                BookEntity(1, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 90)
            )
        )
        cartDao = FakeCartDao(catalogDao)
        orderDao = FakeOrderDao()

        cartRepo = CartRepository(cartDao, catalogDao, session)
        orderRepo = OrderRepository(
            orderDao, cartDao, catalogDao, session,
            SimulatedPaymentGateway(0), fixedClock
        )

        cartDao.upsert(CartItemEntity(1L, 1L, 2))
        val address = AddressEntity(
            id = 1, userId = 1, label = "Home", fullName = "John", phone = "9876543210",
            line1 = "123 Main St", line2 = "", city = "Bengaluru", state = "Karnataka",
            postalCode = "560001", isDefault = true
        )
        val res = orderRepo.checkout(address, PaymentDetails.Card("4242 4242 4242 4242", "John", "12/30", "123"))
        orderId = (res as CheckoutResult.Success).orderId
    }

    @Test
    fun ordersViewModel_loadsOrdersAndSupportsBuyAgain() = runTest {
        val vm = OrdersViewModel(orderRepo, cartRepo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(1, state.orders.size)

        vm.buyAgain(state.orders.first())
        assertEquals(1, cartRepo.lines().first().size)

        collectJob.cancel()
    }

    @Test
    fun orderDetailViewModel_loadsOrderAndCancels() = runTest {
        val vm = OrderDetailViewModel(orderId, orderRepo, cartRepo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.order.collect {} }

        val load = vm.order.value
        assertTrue(load is OrderLoad.Loaded)
        val orderWithItems = (load as OrderLoad.Loaded).order
        assertEquals(orderId, orderWithItems.order.id)

        vm.cancel()
        assertTrue(vm.cancelled)

        collectJob.cancel()
    }
}
