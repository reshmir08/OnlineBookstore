package com.bookstore.app.ui.catalogue

import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.CatalogRepository
import com.bookstore.app.testutil.FakeBrowseDao
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogueViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var browseDao: FakeBrowseDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var session: SessionManager
    private lateinit var catalogRepo: CatalogRepository
    private lateinit var cartRepo: CartRepository

    private val book1 = BookEntity(1, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 90)
    private val book2 = BookEntity(2, "Kotlin in Action", "Dmitry", "Manning", 1, "Desc", 600, 10, 4, 0L, 0L, 85)
    private val book3 = BookEntity(3, "The Alchemist", "Paulo", "Harper", 2, "Desc", 300, 10, 2, 0L, 0L, 95)

    @Before
    fun setUp() {
        catalogDao = FakeCatalogDao(
            initialCategories = listOf(
                CategoryEntity(1, "Tech", "Tech books", "tech"),
                CategoryEntity(2, "Fiction", "Fiction books", "fiction")
            ),
            initialBooks = listOf(book1, book2, book3)
        )
        browseDao = FakeBrowseDao()
        cartDao = FakeCartDao(catalogDao)
        session = SessionManager(initialUserId = 1L)
        catalogRepo = CatalogRepository(catalogDao, browseDao, session)
        cartRepo = CartRepository(cartDao, catalogDao, session)
    }

    @Test
    fun catalogueViewModel_categoryAndBrandSelection() = runTest {
        val vm = CatalogueViewModel(initialCategoryId = 1L, catalog = catalogRepo, cart = cartRepo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(1L, state.selectedCategoryId)
        assertEquals(2, state.books.size)
        assertEquals(listOf("Manning", "Pearson"), state.brands)

        // Select brand
        vm.selectBrand("Pearson")
        assertEquals(1, vm.state.value.books.size)
        assertEquals("Clean Code", vm.state.value.books.first().title)

        // Switch category resets brand
        vm.selectCategory(2L)
        assertEquals(2L, vm.state.value.selectedCategoryId)
        assertEquals(null, vm.state.value.selectedBrand)
        assertEquals(1, vm.state.value.books.size)

        collectJob.cancel()
    }

    @Test
    fun productViewModel_loadsBookAndRelated() = runTest {
        val vm = ProductViewModel(bookId = 1L, catalog = catalogRepo, cart = cartRepo)
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }

        val state = vm.state.value
        assertFalse(state.loading)
        assertNotNull(state.book)
        assertEquals("Clean Code", state.book?.title)
        assertEquals("Tech", state.categoryName)

        // Add to basket
        vm.addToBasket()
        assertEquals(1, cartRepo.lines().first().size)

        collectJob.cancel()
    }
}
