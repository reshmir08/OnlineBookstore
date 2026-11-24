package com.bookstore.app.domain

import com.bookstore.app.data.local.BookEntity

/** Ranks similar books: same author first, then same publisher and same category. */
object RelatedBooks {
    fun forBook(base: BookEntity, catalogue: List<BookEntity>, limit: Int = 8): List<BookEntity> =
        catalogue
            .asSequence()
            .filter { it.id != base.id }
            .map { it to score(base, it) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()

    private fun score(base: BookEntity, other: BookEntity): Double {
        var score = 0.0
        if (other.author == base.author) score += 8
        if (other.categoryId == base.categoryId) score += 4
        if (other.brand == base.brand) score += 2
        // Popularity only breaks ties among books that are genuinely related.
        return if (score > 0) score + other.popularity / 100.0 else 0.0
    }
}
