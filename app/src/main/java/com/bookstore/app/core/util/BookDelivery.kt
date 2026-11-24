package com.bookstore.app.core.util

import com.bookstore.app.data.local.BookEntity
import java.time.LocalDate

/** Tentative delivery date for a single book, counted from [today]. */
fun BookEntity.tentativeDelivery(today: LocalDate = LocalDate.now()): LocalDate =
    DeliveryEstimator.estimate(today, deliveryDays)
