package com.bookstore.app.ui.basket

import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.testutil.FakeCartDao
import com.bookstore.app.testutil.FakeCatalogDao
import com.bookstore.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BasketViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var session: SessionManager
    private lateinit var cartRepo: CartRepository

    private val book1 = BookEntity(1, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 90)

    @Before
    fun setUp() {
        catalogDao = FakeCatalogDao(
            initialCategories = listOf(CategoryEntity(1, "Tech", "Desc", "tech")),
            initialBooks = listOf(book1)
        )
        cartDao = FakeCartDao(catalogDao)
        session = SessionManager(initialUserId = 1L)
        cartRepo = CartRepository(cartDao, catalogDao, session)
    }

    @Test
    fun basketViewModel_stateReflectsCartChanges() = runTest {
        val vm = BasketViewModel(cartRepo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }

        cartRepo.add(1, 2)
        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(1, state.lines.size)
        assertEquals(2, state.itemCount)
        assertEquals(1000L, state.total)

        val line = state.lines.first()
        vm.increase(line)
        assertEquals(3, vm.state.value.itemCount)

        vm.decrease(vm.state.value.lines.first())
        assertEquals(2, vm.state.value.itemCount)

        vm.remove(vm.state.value.lines.first())
        assertEquals(0, vm.state.value.lines.size)

        collectJob.cancel()
    }
}
