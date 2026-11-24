package com.bookstore.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val deliveryFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)

/**
 * Formats a whole-rupee amount with Indian digit grouping, e.g. 123456 -> "₹1,23,456".
 * Grouping is done explicitly so the output is identical on every JVM/Android locale database.
 */
fun Long.asPrice(): String {
    val digits = kotlin.math.abs(this).toString()
    val grouped = if (digits.length <= 3) digits else {
        val head = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        head + "," + digits.takeLast(3)
    }
    return (if (this < 0) "-₹" else "₹") + grouped
}

fun LocalDate.asDeliveryDate(): String = format(deliveryFormatter)

fun Long.asDateTime(zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this).atZone(zone).format(dateTimeFormatter)
