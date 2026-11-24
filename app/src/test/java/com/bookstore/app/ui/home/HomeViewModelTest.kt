package com.bookstore.app.ui.home

import com.bookstore.app.data.auth.AuthRepository
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.payment.PaymentDetails
import com.bookstore.app.data.payment.SimulatedPaymentGateway
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.CatalogRepository
import com.bookstore.app.data.repo.OrderRepository
import com.bookstore.app.data.repo.RecommendationRepository
import com.bookstore.app.testutil.FakeBrowseDao
import com.bookstore.app.testutil.FakeCartDao
import com.bookstore.app.testutil.FakeCatalogDao
import com.bookstore.app.testutil.FakeOrderDao
import com.bookstore.app.testutil.FakeUserDao
import com.bookstore.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userDao: FakeUserDao
    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var browseDao: FakeBrowseDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var orderDao: FakeOrderDao
    private lateinit var session: SessionManager

    private lateinit var authRepo: AuthRepository
    private lateinit var catalogRepo: CatalogRepository
    private lateinit var cartRepo: CartRepository
    private lateinit var orderRepo: OrderRepository
    private lateinit var recRepo: RecommendationRepository

    private val book1 = BookEntity(1, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 90)

    @Before
    fun setUp() = runTest {
        userDao = FakeUserDao()
        session = SessionManager()
        authRepo = AuthRepository(userDao, session)
        authRepo.register("John Doe", "john@test.com", "Password123")

        catalogDao = FakeCatalogDao(
            initialCategories = listOf(CategoryEntity(1, "Tech", "Tech books", "tech")),
            initialBooks = listOf(book1)
        )
        browseDao = FakeBrowseDao()
        cartDao = FakeCartDao(catalogDao)
        orderDao = FakeOrderDao()

        catalogRepo = CatalogRepository(catalogDao, browseDao, session)
        cartRepo = CartRepository(cartDao, catalogDao, session)
        orderRepo = OrderRepository(orderDao, cartDao, catalogDao, session, SimulatedPaymentGateway(0))
        recRepo = RecommendationRepository(catalogRepo, orderRepo, cartRepo)
    }

    @Test
    fun homeViewModel_loadsStateWithUserNameAndCategories() = runTest {
        val vm = HomeViewModel(authRepo, catalogRepo, orderRepo, recRepo, cartRepo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("John", state.userName)
        assertEquals(1, state.categories.size)
        assertEquals("Tech", state.categories.first().name)

        collectJob.cancel()
    }

    @Test
    fun homeViewModel_buyAgainAfterOrder() = runTest {
        // Create an order
        cartDao.upsert(CartItemEntity(1L, 1L, 1))
        val address = AddressEntity(1, 1L, "Home", "John", "123", "Line", "", "City", "State", "560001", true)
        orderRepo.checkout(address, PaymentDetails.Card("4242 4242 4242 4242", "John", "12/30", "123"))

        val vm = HomeViewModel(authRepo, catalogRepo, orderRepo, recRepo, cartRepo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }

        assertEquals(1, vm.state.value.recentOrders.size)
        assertEquals(1, vm.state.value.buyAgain.size)

        vm.buyAgain(vm.state.value.buyAgain.first())
        assertEquals(1, cartDao.linesOnce(1L).size)

        collectJob.cancel()
    }
}
