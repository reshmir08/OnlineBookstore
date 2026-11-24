package com.bookstore.app.ui.checkout

import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.local.PaymentMethod
import com.bookstore.app.data.payment.SimulatedPaymentGateway
import com.bookstore.app.data.repo.AddressRepository
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.OrderRepository
import com.bookstore.app.testutil.FakeAddressDao
import com.bookstore.app.testutil.FakeCartDao
import com.bookstore.app.testutil.FakeCatalogDao
import com.bookstore.app.testutil.FakeOrderDao
import com.bookstore.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var addressDao: FakeAddressDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var orderDao: FakeOrderDao
    private lateinit var session: SessionManager
    private lateinit var addressRepo: AddressRepository
    private lateinit var cartRepo: CartRepository
    private lateinit var orderRepo: OrderRepository

    private val fixedClock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"))
    private var addressId: Long = 0

    @Before
    fun setUp() = runTest {
        session = SessionManager(initialUserId = 1L)
        addressDao = FakeAddressDao()
        catalogDao = FakeCatalogDao(
            initialBooks = listOf(
                BookEntity(1, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 90)
            )
        )
        cartDao = FakeCartDao(catalogDao)
        orderDao = FakeOrderDao()

        addressRepo = AddressRepository(addressDao, session)
        cartRepo = CartRepository(cartDao, catalogDao, session)
        orderRepo = OrderRepository(
            orderDao, cartDao, catalogDao, session,
            SimulatedPaymentGateway(0), fixedClock
        )

        addressId = addressDao.insert(
            AddressEntity(
                id = 0, userId = 1, label = "Home", fullName = "John Doe", phone = "9876543210",
                line1 = "123 Main St", line2 = "", city = "Bengaluru", state = "Karnataka",
                postalCode = "560001", isDefault = true
            )
        )
        cartDao.upsert(CartItemEntity(1L, 1L, 2))
    }

    @Test
    fun paymentViewModel_loadsSummary() = runTest {
        val vm = PaymentViewModel(addressId, addressRepo, cartRepo, orderRepo)
        assertFalse(vm.loading)
        assertEquals(1000L, vm.total)
        assertNotNull(vm.address)
        assertNotNull(vm.deliveryDate)
    }

    @Test
    fun paymentViewModel_cardValidation() = runTest {
        val vm = PaymentViewModel(addressId, addressRepo, cartRepo, orderRepo)
        vm.onMethod(PaymentMethod.CARD)
        vm.pay()

        assertTrue(vm.errors.isNotEmpty())
        assertNull(vm.completedOrderId)

        // Enter valid card
        vm.onCardNumber("4242 4242 4242 4242")
        vm.onCardHolder("John Doe")
        vm.onExpiry("12/30")
        vm.onCvv("123")
        vm.pay()

        assertTrue(vm.errors.isEmpty())
        assertNotNull(vm.completedOrderId)
    }

    @Test
    fun paymentViewModel_upiPayment() = runTest {
        val vm = PaymentViewModel(addressId, addressRepo, cartRepo, orderRepo)
        vm.onMethod(PaymentMethod.UPI)
        vm.onUpi("john@upi")
        vm.pay()

        assertTrue(vm.errors.isEmpty())
        assertNotNull(vm.completedOrderId)
    }
}
