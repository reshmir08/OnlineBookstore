package com.bookstore.app.testutil

import com.bookstore.app.data.local.AddressDao
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.BrowseDao
import com.bookstore.app.data.local.BrowseEventEntity
import com.bookstore.app.data.local.CartDao
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.local.CartLine
import com.bookstore.app.data.local.CatalogDao
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.local.OrderDao
import com.bookstore.app.data.local.OrderEntity
import com.bookstore.app.data.local.OrderItemEntity
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.local.OrderWithItems
import com.bookstore.app.data.local.UserDao
import com.bookstore.app.data.local.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class FakeUserDao : UserDao {
    private val users = mutableListOf<UserEntity>()
    private val idCounter = AtomicLong(1)

    override suspend fun insert(user: UserEntity): Long {
        val id = if (user.id == 0L) idCounter.getAndIncrement() else user.id
        val created = user.copy(id = id)
        users.add(created)
        return id
    }

    override suspend fun update(user: UserEntity) {
        val index = users.indexOfFirst { it.id == user.id || it.email.equals(user.email, ignoreCase = true) }
        if (index != -1) {
            users[index] = user
        } else {
            users.add(user)
        }
    }

    override suspend fun findByEmail(email: String): UserEntity? =
        users.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }

    override suspend fun findById(id: Long): UserEntity? =
        users.firstOrNull { it.id == id }
}

class FakeCatalogDao(
    initialCategories: List<CategoryEntity> = emptyList(),
    initialBooks: List<BookEntity> = emptyList()
) : CatalogDao {
    private val _categories = MutableStateFlow(initialCategories)
    private val _books = MutableStateFlow(initialBooks)

    override suspend fun categoryCount(): Int = _categories.value.size

    override suspend fun insertCategories(categories: List<CategoryEntity>) {
        _categories.value = categories
    }

    override suspend fun insertBooks(books: List<BookEntity>) {
        _books.value = books
    }

    override fun categories(): Flow<List<CategoryEntity>> = _categories

    override fun category(id: Long): Flow<CategoryEntity?> =
        _categories.map { list -> list.firstOrNull { it.id == id } }

    override fun brands(categoryId: Long): Flow<List<String>> =
        _books.map { list -> list.filter { it.categoryId == categoryId }.map { it.brand }.distinct().sorted() }

    override fun booksInCategory(categoryId: Long, brand: String?): Flow<List<BookEntity>> =
        _books.map { list ->
            list.filter { it.categoryId == categoryId && (brand == null || it.brand == brand) }
                .sortedWith(compareByDescending<BookEntity> { it.popularity }.thenBy { it.title })
        }

    override fun allBooks(): Flow<List<BookEntity>> =
        _books.map { it.sortedWith(compareByDescending<BookEntity> { b -> b.popularity }.thenBy { b -> b.title }) }

    override fun book(id: Long): Flow<BookEntity?> =
        _books.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun bookOnce(id: Long): BookEntity? =
        _books.value.firstOrNull { it.id == id }

    override suspend fun decrementStock(id: Long, qty: Int): Int {
        val currentList = _books.value
        val book = currentList.firstOrNull { it.id == id } ?: return 0
        if (book.stock < qty) return 0
        _books.value = currentList.map {
            if (it.id == id) it.copy(stock = it.stock - qty) else it
        }
        return 1
    }

    override suspend fun incrementStock(id: Long, qty: Int) {
        _books.value = _books.value.map {
            if (it.id == id) it.copy(stock = it.stock + qty) else it
        }
    }
}

class FakeCartDao(private val catalogDao: CatalogDao) : CartDao {
    private val _items = MutableStateFlow<List<CartItemEntity>>(emptyList())

    override fun lines(userId: Long): Flow<List<CartLine>> =
        _items.map { items ->
            items.filter { it.userId == userId }
                .mapNotNull { item ->
                    val book = catalogDao.bookOnce(item.bookId) ?: return@mapNotNull null
                    CartLine(item, book)
                }
        }

    override suspend fun linesOnce(userId: Long): List<CartLine> =
        _items.value.filter { it.userId == userId }
            .mapNotNull { item ->
                val book = catalogDao.bookOnce(item.bookId) ?: return@mapNotNull null
                CartLine(item, book)
            }

    override fun count(userId: Long): Flow<Int> =
        _items.map { list -> list.filter { it.userId == userId }.sumOf { it.quantity } }

    override suspend fun item(userId: Long, bookId: Long): CartItemEntity? =
        _items.value.firstOrNull { it.userId == userId && it.bookId == bookId }

    override suspend fun upsert(item: CartItemEntity) {
        val current = _items.value.toMutableList()
        current.removeAll { it.userId == item.userId && it.bookId == item.bookId }
        current.add(item)
        _items.value = current
    }

    override suspend fun delete(userId: Long, bookId: Long) {
        _items.value = _items.value.filterNot { it.userId == userId && it.bookId == bookId }
    }

    override suspend fun clear(userId: Long) {
        _items.value = _items.value.filterNot { it.userId == userId }
    }
}

class FakeAddressDao : AddressDao {
    private val _addresses = MutableStateFlow<List<AddressEntity>>(emptyList())
    private val idGen = AtomicLong(1)

    override fun addresses(userId: Long): Flow<List<AddressEntity>> =
        _addresses.map { list ->
            list.filter { it.userId == userId }
                .sortedWith(compareByDescending<AddressEntity> { it.isDefault }.thenBy { it.id })
        }

    override suspend fun find(id: Long, userId: Long): AddressEntity? =
        _addresses.value.firstOrNull { it.id == id && it.userId == userId }

    override suspend fun count(userId: Long): Int =
        _addresses.value.count { it.userId == userId }

    override suspend fun first(userId: Long): AddressEntity? =
        _addresses.value.firstOrNull { it.userId == userId }

    override suspend fun insert(address: AddressEntity): Long {
        val id = if (address.id == 0L) idGen.getAndIncrement() else address.id
        val newAddr = address.copy(id = id)
        _addresses.value = _addresses.value + newAddr
        return id
    }

    override suspend fun update(address: AddressEntity) {
        _addresses.value = _addresses.value.map { if (it.id == address.id) address else it }
    }

    override suspend fun delete(id: Long, userId: Long) {
        _addresses.value = _addresses.value.filterNot { it.id == id && it.userId == userId }
    }

    override suspend fun clearDefault(userId: Long) {
        _addresses.value = _addresses.value.map {
            if (it.userId == userId) it.copy(isDefault = false) else it
        }
    }

    override suspend fun markDefault(id: Long, userId: Long) {
        _addresses.value = _addresses.value.map {
            if (it.userId == userId && it.id == id) it.copy(isDefault = true) else it
        }
    }
}

class FakeOrderDao : OrderDao {
    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    private val _items = MutableStateFlow<List<OrderItemEntity>>(emptyList())

    override suspend fun insertOrder(order: OrderEntity) {
        _orders.value = _orders.value + order
    }

    override suspend fun insertItems(items: List<OrderItemEntity>) {
        _items.value = _items.value + items
    }

    override fun orders(userId: Long): Flow<List<OrderWithItems>> =
        _orders.map { orders ->
            orders.filter { it.userId == userId }
                .sortedByDescending { it.placedAt }
                .map { order ->
                    val items = _items.value.filter { it.orderId == order.id }
                    OrderWithItems(order, items)
                }
        }

    override fun order(id: String, userId: Long): Flow<OrderWithItems?> =
        _orders.map { orders ->
            val order = orders.firstOrNull { it.id == id && it.userId == userId } ?: return@map null
            val items = _items.value.filter { it.orderId == id }
            OrderWithItems(order, items)
        }

    override suspend fun orderOnce(id: String, userId: Long): OrderWithItems? {
        val order = _orders.value.firstOrNull { it.id == id && it.userId == userId } ?: return null
        val items = _items.value.filter { it.orderId == id }
        return OrderWithItems(order, items)
    }

    override suspend fun updateStatus(id: String, status: OrderStatus, cancelledAt: Long?) {
        _orders.value = _orders.value.map {
            if (it.id == id) it.copy(status = status, cancelledAt = cancelledAt) else it
        }
    }

    override suspend fun exists(id: String): Int =
        _orders.value.count { it.id == id }
}

class FakeBrowseDao : BrowseDao {
    private val _events = MutableStateFlow<List<BrowseEventEntity>>(emptyList())
    private val idGen = AtomicLong(1)

    override suspend fun insert(event: BrowseEventEntity) {
        val id = if (event.id == 0L) idGen.getAndIncrement() else event.id
        _events.value = _events.value + event.copy(id = id)
    }

    override fun recentBookIds(userId: Long, limit: Int): Flow<List<Long>> =
        _events.map { list ->
            list.filter { it.userId == userId }
                .sortedWith(compareByDescending<BrowseEventEntity> { it.viewedAt }.thenByDescending { it.id })
                .take(limit)
                .map { it.bookId }
        }
}
