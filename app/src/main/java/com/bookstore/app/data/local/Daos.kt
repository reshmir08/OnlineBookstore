package com.bookstore.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: Long): UserEntity?
}

@Dao
interface CatalogDao {
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("SELECT * FROM categories ORDER BY id")
    fun categories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun category(id: Long): Flow<CategoryEntity?>

    @Query("SELECT DISTINCT brand FROM books WHERE categoryId = :categoryId ORDER BY brand")
    fun brands(categoryId: Long): Flow<List<String>>

    @Query(
        "SELECT * FROM books WHERE categoryId = :categoryId " +
            "AND (:brand IS NULL OR brand = :brand) ORDER BY popularity DESC, title"
    )
    fun booksInCategory(categoryId: Long, brand: String?): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY popularity DESC, title")
    fun allBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun book(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun bookOnce(id: Long): BookEntity?

    /** Atomically reserves stock; returns 0 when there is not enough. */
    @Query("UPDATE books SET stock = stock - :qty WHERE id = :id AND stock >= :qty")
    suspend fun decrementStock(id: Long, qty: Int): Int

    @Query("UPDATE books SET stock = stock + :qty WHERE id = :id")
    suspend fun incrementStock(id: Long, qty: Int)
}

@Dao
interface CartDao {
    @Transaction
    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY bookId")
    fun lines(userId: Long): Flow<List<CartLine>>

    @Transaction
    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY bookId")
    suspend fun linesOnce(userId: Long): List<CartLine>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE userId = :userId")
    fun count(userId: Long): Flow<Int>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun item(userId: Long, bookId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun delete(userId: Long, bookId: Long)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses WHERE userId = :userId ORDER BY isDefault DESC, id")
    fun addresses(userId: Long): Flow<List<AddressEntity>>

    @Query("SELECT * FROM addresses WHERE id = :id AND userId = :userId")
    suspend fun find(id: Long, userId: Long): AddressEntity?

    @Query("SELECT COUNT(*) FROM addresses WHERE userId = :userId")
    suspend fun count(userId: Long): Int

    @Query("SELECT * FROM addresses WHERE userId = :userId ORDER BY id LIMIT 1")
    suspend fun first(userId: Long): AddressEntity?

    @Insert
    suspend fun insert(address: AddressEntity): Long

    @Update
    suspend fun update(address: AddressEntity)

    @Query("DELETE FROM addresses WHERE id = :id AND userId = :userId")
    suspend fun delete(id: Long, userId: Long)

    @Query("UPDATE addresses SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefault(userId: Long)

    @Query("UPDATE addresses SET isDefault = 1 WHERE id = :id AND userId = :userId")
    suspend fun markDefault(id: Long, userId: Long)
}

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: OrderEntity)

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Transaction
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY placedAt DESC")
    fun orders(userId: Long): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id AND userId = :userId")
    fun order(id: String, userId: Long): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id AND userId = :userId")
    suspend fun orderOnce(id: String, userId: Long): OrderWithItems?

    @Query("UPDATE orders SET status = :status, cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: OrderStatus, cancelledAt: Long?)

    @Query("SELECT COUNT(*) FROM orders WHERE id = :id")
    suspend fun exists(id: String): Int
}

@Dao
interface BrowseDao {
    @Insert
    suspend fun insert(event: BrowseEventEntity)

    @Query("SELECT bookId FROM browse_events WHERE userId = :userId ORDER BY viewedAt DESC LIMIT :limit")
    fun recentBookIds(userId: Long, limit: Int): Flow<List<Long>>
}
