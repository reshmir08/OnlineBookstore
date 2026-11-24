package com.bookstore.app.data.repo

import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.CartDao
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.local.CartLine
import com.bookstore.app.data.local.CatalogDao
import com.bookstore.app.data.local.OrderItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

private const val MAX_QTY_PER_BOOK = 10

class CartRepository(
    private val cartDao: CartDao,
    private val catalogDao: CatalogDao,
    private val session: SessionManager
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun lines(): Flow<List<CartLine>> =
        session.userId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else cartDao.lines(id) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun itemCount(): Flow<Int> =
        session.userId.flatMapLatest { id -> if (id == null) flowOf(0) else cartDao.count(id) }

    suspend fun add(bookId: Long, quantity: Int = 1): AppResult<Unit> {
        val userId = session.userId.value ?: return AppResult.Failure("Please sign in first")
        val book = catalogDao.bookOnce(bookId) ?: return AppResult.Failure("This book is no longer available")
        if (book.stock <= 0) return AppResult.Failure("\"${book.title}\" is out of stock")
        val current = cartDao.item(userId, bookId)?.quantity ?: 0
        val limit = minOf(book.stock, MAX_QTY_PER_BOOK)
        if (current >= limit) return AppResult.Failure("Maximum available quantity already in your basket")
        cartDao.upsert(CartItemEntity(userId, bookId, minOf(current + quantity, limit)))
        return AppResult.Success(Unit)
    }

    suspend fun setQuantity(bookId: Long, quantity: Int): AppResult<Unit> {
        val userId = session.userId.value ?: return AppResult.Failure("Please sign in first")
        if (quantity <= 0) {
            cartDao.delete(userId, bookId)
            return AppResult.Success(Unit)
        }
        val book = catalogDao.bookOnce(bookId) ?: return AppResult.Failure("This book is no longer available")
        val limit = minOf(book.stock, MAX_QTY_PER_BOOK)
        if (quantity > limit) return AppResult.Failure("Only $limit copies available")
        cartDao.upsert(CartItemEntity(userId, bookId, quantity))
        return AppResult.Success(Unit)
    }

    suspend fun remove(bookId: Long) {
        session.userId.value?.let { cartDao.delete(it, bookId) }
    }

    /** Re-adds previously ordered items (Buy Again). Returns how many titles could be added. */
    suspend fun buyAgain(items: List<OrderItemEntity>): AppResult<Int> {
        var added = 0
        var lastError: String? = null
        for (item in items) {
            when (val result = add(item.bookId, item.quantity)) {
                is AppResult.Success -> added++
                is AppResult.Failure -> lastError = result.message
            }
        }
        return if (added > 0) AppResult.Success(added) else AppResult.Failure(lastError ?: "Nothing to add")
    }
}
