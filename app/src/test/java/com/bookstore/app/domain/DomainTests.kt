package com.bookstore.app.domain

import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.OrderEntity
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.local.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

private fun book(
    id: Long, category: Long = 1, brand: String = "Pub", author: String = "Author $id",
    stock: Int = 10, popularity: Int = 50
) = BookEntity(id, "Book $id", author, brand, category, "d", 100, stock, 3, 0xFF000000, 0xFF000000, popularity)

private fun order(placedAt: Long, status: OrderStatus = OrderStatus.PLACED) = OrderEntity(
    id = "O1", userId = 1, status = status, placedAt = placedAt, cancelledAt = null, total = 100,
    estimatedDeliveryEpochDay = 0, addressName = "n", addressPhone = "p", addressText = "a",
    paymentMethod = PaymentMethod.UPI, paymentDetail = "d", transactionId = "t", paidAt = placedAt
)

class CancellationPolicyTest {
    private val placed = 1_700_000_000_000L
    private fun hours(h: Long) = TimeUnit.HOURS.toMillis(h)

    @Test fun eligibleJustAfterPlacing() =
        assertTrue(CancellationPolicy.isEligible(order(placed), placed + 1))

    @Test fun eligibleAt47Hours() =
        assertTrue(CancellationPolicy.isEligible(order(placed), placed + hours(47)))

    @Test fun eligibleExactlyAt48Hours() =
        assertTrue(CancellationPolicy.isEligible(order(placed), placed + hours(48)))

    @Test fun notEligibleOneMillisecondAfter48Hours() =
        assertFalse(CancellationPolicy.isEligible(order(placed), placed + hours(48) + 1))

    @Test fun notEligibleAfterSeveralDays() =
        assertFalse(CancellationPolicy.isEligible(order(placed), placed + hours(24 * 5)))

    @Test fun alreadyCancelledIsNotEligible() =
        assertFalse(CancellationPolicy.isEligible(order(placed, OrderStatus.CANCELLED), placed + hours(1)))

    @Test fun remainingLabelDescribesTimeLeft() {
        assertEquals("1 day 3 hrs left", CancellationPolicy.remainingLabel(order(placed), placed + hours(21)))
        assertEquals("5 hrs 0 mins left", CancellationPolicy.remainingLabel(order(placed), placed + hours(43)))
        assertEquals("30 mins left", CancellationPolicy.remainingLabel(order(placed), placed + hours(48) - TimeUnit.MINUTES.toMillis(30)))
    }

    @Test fun remainingNeverNegative() =
        assertEquals(0L, CancellationPolicy.remainingMillis(order(placed), placed + hours(100)))
}

class RecommendationEngineTest {
    private val names = mapOf(1L to "Fiction", 2L to "Tech", 3L to "Kids")

    @Test fun coldStartReturnsMostPopularInStock() {
        val catalogue = listOf(
            book(1, popularity = 10), book(2, popularity = 90), book(3, popularity = 70, stock = 0)
        )
        val recs = RecommendationEngine.recommend(catalogue, names, emptyList(), emptyList(), emptyList())
        assertEquals(listOf(2L, 1L), recs.map { it.book.id })
    }

    @Test fun purchasedCategoryIsBoosted() {
        val purchased = book(10, category = 2)
        val catalogue = listOf(
            purchased, book(11, category = 2, popularity = 40), book(12, category = 1, popularity = 99)
        )
        val recs = RecommendationEngine.recommend(catalogue, names, listOf(purchased), emptyList(), emptyList())
        assertEquals(11L, recs.first().book.id)
        assertTrue(recs.first().reason.contains("Tech"))
    }

    @Test fun alreadyPurchasedBooksAreExcluded() {
        val purchased = book(10, category = 2)
        val recs = RecommendationEngine.recommend(
            listOf(purchased, book(11, category = 2)), names, listOf(purchased), emptyList(), emptyList()
        )
        assertTrue(recs.none { it.book.id == 10L })
    }

    @Test fun browsingInfluencesRecommendations() {
        val viewed = book(20, category = 3)
        val catalogue = listOf(viewed, book(21, category = 3, popularity = 30), book(22, category = 1, popularity = 95))
        val recs = RecommendationEngine.recommend(catalogue, names, emptyList(), listOf(viewed), emptyList())
        assertEquals(21L, recs.first().book.id)
        assertTrue(recs.none { it.book.id == 20L })
        assertTrue(recs.first().reason.contains("interest"))
    }

    @Test fun purchasesOutweighBrowsing() {
        val bought = book(30, category = 1)
        val viewed = book(31, category = 2)
        val catalogue = listOf(bought, viewed, book(32, category = 1), book(33, category = 2))
        val recs = RecommendationEngine.recommend(catalogue, names, listOf(bought), listOf(viewed), emptyList())
        assertEquals(32L, recs.first().book.id)
    }

    @Test fun outOfStockBooksNeverRecommended() {
        val bought = book(40, category = 1)
        val recs = RecommendationEngine.recommend(
            listOf(bought, book(41, category = 1, stock = 0)), names, listOf(bought), emptyList(), emptyList()
        )
        assertTrue(recs.isEmpty())
    }

    @Test fun respectsLimit() {
        val bought = book(50, category = 1)
        val catalogue = listOf(bought) + (51L..70L).map { book(it, category = 1) }
        val recs = RecommendationEngine.recommend(catalogue, names, listOf(bought), emptyList(), emptyList(), limit = 5)
        assertEquals(5, recs.size)
    }
}

class RelatedBooksTest {
    @Test fun excludesSelfAndUnrelated() {
        val base = book(1, category = 1, brand = "A")
        val related = RelatedBooks.forBook(base, listOf(base, book(2, category = 1), book(3, category = 9, brand = "Z")))
        assertEquals(listOf(2L), related.map { it.id })
    }

    @Test fun sameAuthorRanksFirst() {
        val base = book(1, category = 1, brand = "A", author = "Same")
        val sameAuthor = book(2, category = 5, brand = "Q", author = "Same", popularity = 1)
        val sameCategory = book(3, category = 1, brand = "A", popularity = 100)
        val related = RelatedBooks.forBook(base, listOf(base, sameCategory, sameAuthor))
        assertEquals(listOf(2L, 3L), related.map { it.id })
    }
}
