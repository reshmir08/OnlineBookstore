package com.bookstore.app.data.repo

import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.local.OrderWithItems
import com.bookstore.app.domain.Recommendation
import com.bookstore.app.domain.RecommendationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Builds personalised recommendations from order history, browsing history and the basket. */
class RecommendationRepository(
    private val catalog: CatalogRepository,
    private val orders: OrderRepository,
    private val cart: CartRepository
) {
    fun recommendations(limit: Int = 10): Flow<List<Recommendation>> =
        combine(
            catalog.allBooks(), catalog.categories(), orders.orders(), catalog.recentlyViewedIds(), cart.lines()
        ) { books, categories, orderList, viewed, cartLines ->
            val byId = books.associateBy { it.id }
            RecommendationEngine.recommend(
                catalogue = books,
                categoryNames = categories.associate { it.id to it.name },
                purchased = orderList.purchasedBooks(byId),
                browsed = viewed.mapNotNull { byId[it] },
                inCart = cartLines.map { it.book },
                limit = limit
            )
        }

    private fun List<OrderWithItems>.purchasedBooks(byId: Map<Long, BookEntity>) =
        filter { it.order.status != OrderStatus.CANCELLED }
            .flatMap { it.items }
            .mapNotNull { byId[it.bookId] }
}
