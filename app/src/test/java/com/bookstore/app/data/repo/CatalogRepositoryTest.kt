package com.bookstore.app.data.repo

import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.testutil.FakeBrowseDao
import com.bookstore.app.testutil.FakeCatalogDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CatalogRepositoryTest {
    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var browseDao: FakeBrowseDao
    private lateinit var session: SessionManager
    private lateinit var catalogRepo: CatalogRepository

    private val cat1 = CategoryEntity(1, "Tech", "Tech books", "tech")
    private val cat2 = CategoryEntity(2, "Fiction", "Fiction books", "fiction")

    private val book1 = BookEntity(1, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 90)
    private val book2 = BookEntity(2, "Kotlin in Action", "Dmitry", "Manning", 1, "Desc", 600, 10, 4, 0L, 0L, 85)
    private val book3 = BookEntity(3, "The Alchemist", "Paulo", "Harper", 2, "Desc", 300, 10, 2, 0L, 0L, 95)

    @Before
    fun setUp() {
        catalogDao = FakeCatalogDao(
            initialCategories = listOf(cat1, cat2),
            initialBooks = listOf(book1, book2, book3)
        )
        browseDao = FakeBrowseDao()
        session = SessionManager(initialUserId = 5L)
        catalogRepo = CatalogRepository(catalogDao, browseDao, session)
    }

    @Test
    fun categories_returnsAllCategories() = runTest {
        val categories = catalogRepo.categories().first()
        assertEquals(2, categories.size)
        assertEquals("Tech", categories[0].name)
        assertEquals("Fiction", categories[1].name)
    }

    @Test
    fun categoryById_returnsMatchingCategory() = runTest {
        val category = catalogRepo.category(1).first()
        assertNotNull(category)
        assertEquals("Tech", category?.name)

        val nonExistent = catalogRepo.category(99).first()
        assertNull(nonExistent)
    }

    @Test
    fun brands_returnsUniqueBrandsForCategory() = runTest {
        val brands = catalogRepo.brands(1).first()
        assertEquals(listOf("Manning", "Pearson"), brands)
    }

    @Test
    fun booksInCategory_filtersCorrectly() = runTest {
        val allTech = catalogRepo.books(1, null).first()
        assertEquals(2, allTech.size)

        val pearsonTech = catalogRepo.books(1, "Pearson").first()
        assertEquals(1, pearsonTech.size)
        assertEquals("Clean Code", pearsonTech.first().title)
    }

    @Test
    fun recordViewAndRecentlyViewedIds() = runTest {
        catalogRepo.recordView(1)
        catalogRepo.recordView(3)

        val recent = catalogRepo.recentlyViewedIds(5).first()
        assertEquals(listOf(3L, 1L), recent)
    }

    @Test
    fun recentlyViewed_emptyWhenSignedOut() = runTest {
        catalogRepo.recordView(1)
        session.signOut()

        val recent = catalogRepo.recentlyViewedIds(5).first()
        assertTrue(recent.isEmpty())
    }
}
