package com.bookstore.app.domain

import com.bookstore.app.data.local.BookEntity

data class Recommendation(val book: BookEntity, val reason: String)

/**
 * Scores catalogue books against the customer's history. Signals:
 *  - purchases (strongest): category and publisher of ordered books
 *  - browsing: category and publisher of recently viewed books
 *  - cart: category of books currently in the basket
 * Books already purchased (they appear under "Buy Again"), viewed or in the basket are excluded.
 * With no history, the most popular books are returned.
 */
object RecommendationEngine {
    private const val PURCHASE_CATEGORY_WEIGHT = 5.0
    private const val PURCHASE_BRAND_WEIGHT = 3.0
    private const val BROWSE_CATEGORY_WEIGHT = 2.0
    private const val BROWSE_BRAND_WEIGHT = 1.0
    private const val CART_CATEGORY_WEIGHT = 1.5

    fun recommend(
        catalogue: List<BookEntity>,
        categoryNames: Map<Long, String>,
        purchased: List<BookEntity>,
        browsed: List<BookEntity>,
        inCart: List<BookEntity>,
        limit: Int = 10
    ): List<Recommendation> {
        // Books the customer already bought, just viewed, or has in the basket are not suggested again.
        val excludedIds = (purchased + browsed + inCart).map { it.id }.toSet()
        val candidates = catalogue.filter { it.stock > 0 && it.id !in excludedIds }

        if (purchased.isEmpty() && browsed.isEmpty() && inCart.isEmpty()) {
            return candidates.sortedByDescending { it.popularity }.take(limit)
                .map { Recommendation(it, "Popular right now") }
        }

        val categoryScore = HashMap<Long, Double>()
        val brandScore = HashMap<String, Double>()
        purchased.forEach {
            categoryScore.merge(it.categoryId, PURCHASE_CATEGORY_WEIGHT, Double::plus)
            brandScore.merge(it.brand, PURCHASE_BRAND_WEIGHT, Double::plus)
        }
        browsed.forEach {
            categoryScore.merge(it.categoryId, BROWSE_CATEGORY_WEIGHT, Double::plus)
            brandScore.merge(it.brand, BROWSE_BRAND_WEIGHT, Double::plus)
        }
        inCart.forEach { categoryScore.merge(it.categoryId, CART_CATEGORY_WEIGHT, Double::plus) }

        val purchasedCategories = purchased.map { it.categoryId }.toSet()
        val browsedCategories = browsed.map { it.categoryId }.toSet()

        return candidates
            .map { book ->
                val score = (categoryScore[book.categoryId] ?: 0.0) +
                    (brandScore[book.brand] ?: 0.0) + book.popularity / 100.0
                book to score
            }
            .filter { (book, _) -> categoryScore.containsKey(book.categoryId) || brandScore.containsKey(book.brand) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (book, _) ->
                val categoryName = categoryNames[book.categoryId] ?: "this category"
                val reason = when {
                    book.categoryId in purchasedCategories -> "Because you bought $categoryName books"
                    book.brand in purchased.map { it.brand } -> "More from ${book.brand}, a publisher you bought"
                    book.categoryId in browsedCategories -> "Based on your interest in $categoryName"
                    else -> "Picked for you"
                }
                Recommendation(book, reason)
            }
    }
}
