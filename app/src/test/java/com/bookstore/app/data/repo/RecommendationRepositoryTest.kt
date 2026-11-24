package com.bookstore.app.data.repo

import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.payment.PaymentDetails
import com.bookstore.app.data.payment.SimulatedPaymentGateway
import com.bookstore.app.testutil.FakeBrowseDao
import com.bookstore.app.testutil.FakeCartDao
import com.bookstore.app.testutil.FakeCatalogDao
import com.bookstore.app.testutil.FakeOrderDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationRepositoryTest {
    private lateinit var catalogDao: FakeCatalogDao
    private lateinit var browseDao: FakeBrowseDao
    private lateinit var cartDao: FakeCartDao
    private lateinit var orderDao: FakeOrderDao
    private lateinit var session: SessionManager

    private lateinit var catalogRepo: CatalogRepository
    private lateinit var cartRepo: CartRepository
    private lateinit var orderRepo: OrderRepository
    private lateinit var recommendationRepo: RecommendationRepository

    private val catTech = CategoryEntity(1, "Tech", "Tech books", "tech")
    private val catFiction = CategoryEntity(2, "Fiction", "Fiction books", "fiction")

    private val bookTech1 = BookEntity(101, "Clean Code", "Uncle Bob", "Pearson", 1, "Desc", 500, 10, 3, 0L, 0L, 80)
    private val bookTech2 = BookEntity(102, "Kotlin in Action", "Dmitry", "Manning", 1, "Desc", 600, 10, 4, 0L, 0L, 70)
    private val bookFiction1 = BookEntity(201, "The Alchemist", "Paulo", "Harper", 2, "Desc", 300, 10, 2, 0L, 0L, 95)
    private val bookFiction2 = BookEntity(202, "Midnight Library", "Matt", "Penguin", 2, "Desc", 400, 10, 3, 0L, 0L, 90)

    @Before
    fun setUp() {
        catalogDao = FakeCatalogDao(
            initialCategories = listOf(catTech, catFiction),
            initialBooks = listOf(bookTech1, bookTech2, bookFiction1, bookFiction2)
        )
        browseDao = FakeBrowseDao()
        cartDao = FakeCartDao(catalogDao)
        orderDao = FakeOrderDao()
        session = SessionManager(initialUserId = 10L)

        catalogRepo = CatalogRepository(catalogDao, browseDao, session)
        cartRepo = CartRepository(cartDao, catalogDao, session)
        orderRepo = OrderRepository(orderDao, cartDao, catalogDao, session, SimulatedPaymentGateway(0))
        recommendationRepo = RecommendationRepository(catalogRepo, orderRepo, cartRepo)
    }

    @Test
    fun coldStart_recommendsTopPopularInStock() = runTest {
        val recs = recommendationRepo.recommendations(limit = 2).first()
        assertEquals(2, recs.size)
        // Most popular first: 201 (pop 95), then 202 (pop 90)
        assertEquals(201L, recs[0].book.id)
        assertEquals(202L, recs[1].book.id)
    }

    @Test
    fun withPurchases_boostsPurchasedCategory() = runTest {
        // Buy tech book 101
        cartDao.upsert(CartItemEntity(10L, 101L, 1))
        val address = AddressEntity(1, 10L, "Home", "Name", "123", "Line", "", "City", "State", "560001", true)
        orderRepo.checkout(address, PaymentDetails.Card("4242 4242 4242 4242", "Name", "12/30", "123"))

        val recs = recommendationRepo.recommendations().first()
        // 101 is already purchased, so 102 should now be boosted ahead because category 1 was purchased
        assertEquals(102L, recs.first().book.id)
        assertTrue(recs.first().reason.contains("Tech"))
    }
}
