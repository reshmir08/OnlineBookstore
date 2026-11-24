package com.bookstore.app.data.repo

import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.BrowseDao
import com.bookstore.app.data.local.BrowseEventEntity
import com.bookstore.app.data.local.CatalogDao
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.domain.RelatedBooks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class CatalogRepository(
    private val catalogDao: CatalogDao,
    private val browseDao: BrowseDao,
    private val session: SessionManager
) {
    fun categories(): Flow<List<CategoryEntity>> = catalogDao.categories()
    fun category(id: Long): Flow<CategoryEntity?> = catalogDao.category(id)
    fun brands(categoryId: Long): Flow<List<String>> = catalogDao.brands(categoryId)
    fun books(categoryId: Long, brand: String?): Flow<List<BookEntity>> =
        catalogDao.booksInCategory(categoryId, brand)
    fun allBooks(): Flow<List<BookEntity>> = catalogDao.allBooks()
    fun book(id: Long): Flow<BookEntity?> = catalogDao.book(id)

    fun related(book: BookEntity): Flow<List<BookEntity>> =
        catalogDao.allBooks().map { RelatedBooks.forBook(book, it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun recentlyViewedIds(limit: Int = 20): Flow<List<Long>> =
        session.userId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else browseDao.recentBookIds(id, limit)
        }

    /** Records a product view; it feeds the "browsing preferences" signal for recommendations. */
    suspend fun recordView(bookId: Long) {
        val userId = session.userId.value ?: return
        browseDao.insert(BrowseEventEntity(userId = userId, bookId = bookId, viewedAt = System.currentTimeMillis()))
    }
}
