package com.bookstore.app.domain

import com.bookstore.app.data.local.OrderEntity
import com.bookstore.app.data.local.OrderStatus
import java.util.concurrent.TimeUnit

/** Orders can be cancelled up to 48 hours after the order placement timestamp. */
object CancellationPolicy {
    val WINDOW_MILLIS: Long = TimeUnit.HOURS.toMillis(48)

    fun deadline(order: OrderEntity): Long = order.placedAt + WINDOW_MILLIS

    fun isEligible(order: OrderEntity, nowMillis: Long): Boolean =
        order.status == OrderStatus.PLACED && nowMillis - order.placedAt <= WINDOW_MILLIS

    fun remainingMillis(order: OrderEntity, nowMillis: Long): Long =
        (deadline(order) - nowMillis).coerceAtLeast(0)

    /** Human-friendly time left, e.g. "1 day 3 hrs left" or "45 mins left". */
    fun remainingLabel(order: OrderEntity, nowMillis: Long): String {
        val remaining = remainingMillis(order, nowMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
        return when {
            hours >= 24 -> "${hours / 24} day ${hours % 24} hrs left"
            hours >= 1 -> "$hours hrs $minutes mins left"
            else -> "$minutes mins left"
        }
    }
}
