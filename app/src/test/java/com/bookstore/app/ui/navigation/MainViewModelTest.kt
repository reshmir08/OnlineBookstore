package com.bookstore.app.ui.navigation

import com.bookstore.app.data.auth.AuthRepository
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.testutil.FakeCartDao
import com.bookstore.app.testutil.FakeCatalogDao
import com.bookstore.app.testutil.FakeUserDao
import com.bookstore.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userDao: FakeUserDao
    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var session: SessionManager
    private lateinit var authRepo: AuthRepository
    private lateinit var cartRepo: CartRepository

    @Before
    fun setUp() {
        userDao = FakeUserDao()
        catalogDao = FakeCatalogDao(
            initialBooks = listOf(
                BookEntity(1, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 90)
            )
        )
        cartDao = FakeCartDao(catalogDao)
        session = SessionManager(initialUserId = 1L)
        authRepo = AuthRepository(userDao, session)
        cartRepo = CartRepository(cartDao, catalogDao, session)
    }

    @Test
    fun mainViewModel_cartCountAndLogout() = runTest {
        val vm = MainViewModel(cartRepo, authRepo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.cartCount.collect {} }

        assertEquals(0, vm.cartCount.value)

        cartDao.upsert(CartItemEntity(1L, 1L, 3))
        assertEquals(3, vm.cartCount.value)

        vm.logout()
        assertNull(session.userId.value)

        collectJob.cancel()
    }
}
