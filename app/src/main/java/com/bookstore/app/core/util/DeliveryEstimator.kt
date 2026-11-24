package com.bookstore.app.core.util

import java.time.DayOfWeek
import java.time.LocalDate

/** Computes tentative delivery dates, counting only business days (Mon-Fri). */
object DeliveryEstimator {
    fun estimate(from: LocalDate, businessDays: Int): LocalDate {
        var date = from
        var remaining = businessDays
        while (remaining > 0) {
            date = date.plusDays(1)
            if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) remaining--
        }
        return date
    }
}
