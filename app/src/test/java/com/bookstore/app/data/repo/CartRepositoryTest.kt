package com.bookstore.app.data.repo

import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.local.OrderItemEntity
import com.bookstore.app.testutil.FakeCartDao
import com.bookstore.app.testutil.FakeCatalogDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CartRepositoryTest {
    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var session: SessionManager
    private lateinit var cartRepo: CartRepository

    private val testBook1 = BookEntity(
        id = 1, title = "Clean Code", author = "Uncle Bob", brand = "Pearson",
        categoryId = 1, description = "A handbook", price = 500, stock = 5,
        deliveryDays = 3, coverStart = 0L, coverEnd = 0L, popularity = 80
    )
    private val testBook2 = BookEntity(
        id = 2, title = "Out of Stock Book", author = "Author", brand = "Publisher",
        categoryId = 1, description = "Desc", price = 300, stock = 0,
        deliveryDays = 2, coverStart = 0L, coverEnd = 0L, popularity = 50
    )
    private val testBook3 = BookEntity(
        id = 3, title = "High Stock Book", author = "Author 3", brand = "Publisher",
        categoryId = 1, description = "Desc", price = 200, stock = 50,
        deliveryDays = 2, coverStart = 0L, coverEnd = 0L, popularity = 60
    )

    @Before
    fun setUp() = runTest {
        catalogDao = FakeCatalogDao(
            initialCategories = listOf(CategoryEntity(1, "Fiction", "Desc", "fiction")),
            initialBooks = listOf(testBook1, testBook2, testBook3)
        )
        cartDao = FakeCartDao(catalogDao)
        session = SessionManager(initialUserId = 10L)
        cartRepo = CartRepository(cartDao, catalogDao, session)
    }

    @Test
    fun signedOut_addFails() = runTest {
        session.signOut()
        val result = cartRepo.add(1, 1)
        assertTrue(result is AppResult.Failure)
        assertEquals("Please sign in first", (result as AppResult.Failure).message)
    }

    @Test
    fun outOfStock_addFails() = runTest {
        val result = cartRepo.add(2, 1)
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).message.contains("out of stock"))
    }

    @Test
    fun nonexistentBook_addFails() = runTest {
        val result = cartRepo.add(999, 1)
        assertTrue(result is AppResult.Failure)
        assertEquals("This book is no longer available", (result as AppResult.Failure).message)
    }

    @Test
    fun validAdd_increasesCountAndShowsInLines() = runTest {
        val result = cartRepo.add(1, 2)
        assertTrue(result is AppResult.Success)

        val lines = cartRepo.lines().first()
        assertEquals(1, lines.size)
        assertEquals(1L, lines.first().book.id)
        assertEquals(2, lines.first().item.quantity)
        assertEquals(1000L, lines.first().lineTotal)

        val count = cartRepo.itemCount().first()
        assertEquals(2, count)
    }

    @Test
    fun addExceedingStock_capsAtStock() = runTest {
        cartRepo.add(1, 4)
        val result = cartRepo.add(1, 2) // stock is 5
        assertTrue(result is AppResult.Success)
        val lines = cartRepo.lines().first()
        assertEquals(5, lines.first().item.quantity)

        val nextAdd = cartRepo.add(1, 1)
        assertTrue(nextAdd is AppResult.Failure)
    }

    @Test
    fun addCapsAtMaxLimit10() = runTest {
        cartRepo.add(3, 8)
        cartRepo.add(3, 5) // stock is 50, but max is 10
        val lines = cartRepo.lines().first()
        assertEquals(10, lines.first().item.quantity)
    }

    @Test
    fun setQuantity_zeroRemovesItem() = runTest {
        cartRepo.add(1, 2)
        assertEquals(1, cartRepo.lines().first().size)

        val result = cartRepo.setQuantity(1, 0)
        assertTrue(result is AppResult.Success)
        assertEquals(0, cartRepo.lines().first().size)
    }

    @Test
    fun setQuantity_validQuantityUpdates() = runTest {
        cartRepo.add(1, 1)
        val result = cartRepo.setQuantity(1, 4)
        assertTrue(result is AppResult.Success)
        assertEquals(4, cartRepo.lines().first().first().item.quantity)
    }

    @Test
    fun setQuantity_exceedingStockFails() = runTest {
        cartRepo.add(1, 1)
        val result = cartRepo.setQuantity(1, 10) // stock is 5
        assertTrue(result is AppResult.Failure)
        assertEquals("Only 5 copies available", (result as AppResult.Failure).message)
    }

    @Test
    fun remove_deletesBookFromCart() = runTest {
        cartRepo.add(1, 2)
        cartRepo.remove(1)
        assertEquals(0, cartRepo.lines().first().size)
    }

    @Test
    fun buyAgain_addsAvailableItems() = runTest {
        val items = listOf(
            OrderItemEntity(1, "O1", 1, "Clean Code", "Uncle Bob", "Pearson", 500, 2, 0L, 0L),
            OrderItemEntity(2, "O1", 2, "Out of Stock Book", "Author", "Publisher", 300, 1, 0L, 0L)
        )
        val result = cartRepo.buyAgain(items)
        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).value) // only 1 was available
        assertEquals(2, cartRepo.lines().first().first().item.quantity)
    }
}
