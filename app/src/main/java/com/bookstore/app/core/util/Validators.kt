package com.bookstore.app.core.util

import java.time.YearMonth

/** Input validators. Each returns an error message, or null when the input is valid. */
object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val UPI = Regex("^[A-Za-z0-9.\\-_]{2,}@[A-Za-z]{2,}$")

    fun required(value: String, label: String): String? =
        if (value.isBlank()) "$label is required" else null

    fun name(value: String): String? = when {
        value.isBlank() -> "Name is required"
        value.trim().length < 2 -> "Name is too short"
        else -> null
    }

    fun email(value: String): String? = when {
        value.isBlank() -> "Email is required"
        !EMAIL.matches(value.trim()) -> "Enter a valid email address"
        else -> null
    }

    fun password(value: String): String? = when {
        value.isEmpty() -> "Password is required"
        value.length < 8 -> "Use at least 8 characters"
        value.none { it.isLetter() } || value.none { it.isDigit() } ->
            "Use both letters and numbers"
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Confirm your password"
        password != confirm -> "Passwords do not match"
        else -> null
    }

    fun phone(value: String): String? {
        val digits = value.filter { it.isDigit() }
        return when {
            value.isBlank() -> "Phone number is required"
            digits.length !in 10..12 -> "Enter a valid phone number"
            else -> null
        }
    }

    fun postalCode(value: String): String? = when {
        value.isBlank() -> "PIN code is required"
        !Regex("^\\d{6}$").matches(value.trim()) -> "Enter a 6-digit PIN code"
        else -> null
    }

    fun cardNumber(value: String): String? {
        val digits = value.filter { it.isDigit() }
        return when {
            digits.isEmpty() -> "Card number is required"
            digits.length !in 13..19 || !passesLuhn(digits) -> "Enter a valid card number"
            else -> null
        }
    }

    /** Expiry in MM/YY form. */
    fun cardExpiry(value: String, now: YearMonth): String? {
        val match = Regex("^(\\d{2})/(\\d{2})$").matchEntire(value.trim())
            ?: return "Use MM/YY format"
        val month = match.groupValues[1].toInt()
        val year = 2000 + match.groupValues[2].toInt()
        if (month !in 1..12) return "Invalid month"
        return if (YearMonth.of(year, month).isBefore(now)) "Card has expired" else null
    }

    fun cardCvv(value: String): String? =
        if (Regex("^\\d{3,4}$").matches(value)) null else "Enter a 3 or 4 digit CVV"

    fun upiId(value: String): String? = when {
        value.isBlank() -> "UPI ID is required"
        !UPI.matches(value.trim()) -> "Enter a valid UPI ID (name@bank)"
        else -> null
    }

    fun passesLuhn(digits: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in digits.indices.reversed()) {
            var n = digits[i] - '0'
            if (n !in 0..9) return false
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}
