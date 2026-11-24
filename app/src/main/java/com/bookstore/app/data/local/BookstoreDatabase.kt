package com.bookstore.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        BookEntity::class,
        CartItemEntity::class,
        AddressEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        BrowseEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BookstoreDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun catalogDao(): CatalogDao
    abstract fun cartDao(): CartDao
    abstract fun addressDao(): AddressDao
    abstract fun orderDao(): OrderDao
    abstract fun browseDao(): BrowseDao
}
