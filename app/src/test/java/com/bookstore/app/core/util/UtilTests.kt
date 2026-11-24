package com.bookstore.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ValidatorsTest {

    @Test fun email_acceptsValidAndRejectsInvalid() {
        assertNull(Validators.email("reader@example.com"))
        assertNull(Validators.email("  first.last+tag@mail.co.in "))
        assertNotNull(Validators.email(""))
        assertNotNull(Validators.email("not-an-email"))
        assertNotNull(Validators.email("a@b"))
    }

    @Test fun password_requiresLengthLettersAndDigits() {
        assertNull(Validators.password("Books1234"))
        assertNotNull(Validators.password(""))
        assertNotNull(Validators.password("short1"))
        assertNotNull(Validators.password("onlyletters"))
        assertNotNull(Validators.password("12345678"))
    }

    @Test fun confirmPassword_mustMatch() {
        assertNull(Validators.confirmPassword("Books1234", "Books1234"))
        assertNotNull(Validators.confirmPassword("Books1234", "Books12345"))
        assertNotNull(Validators.confirmPassword("Books1234", ""))
    }

    @Test fun postalCode_isSixDigits() {
        assertNull(Validators.postalCode("560001"))
        assertNotNull(Validators.postalCode("5600"))
        assertNotNull(Validators.postalCode("56000a"))
        assertNotNull(Validators.postalCode(""))
    }

    @Test fun phone_acceptsTenToTwelveDigits() {
        assertNull(Validators.phone("9876543210"))
        assertNull(Validators.phone("+91 98765 43210"))
        assertNotNull(Validators.phone("12345"))
        assertNotNull(Validators.phone(""))
    }

    @Test fun cardNumber_usesLuhn() {
        assertNull(Validators.cardNumber("4242 4242 4242 4242"))
        assertNull(Validators.cardNumber("4000 0000 0000 0002"))
        assertNotNull(Validators.cardNumber("4242 4242 4242 4241"))
        assertNotNull(Validators.cardNumber("1234"))
        assertNotNull(Validators.cardNumber(""))
    }

    @Test fun cardExpiry_rejectsPastAndMalformed() {
        val now = YearMonth.of(2025, 6)
        assertNull(Validators.cardExpiry("06/25", now))
        assertNull(Validators.cardExpiry("12/30", now))
        assertNotNull(Validators.cardExpiry("05/25", now))
        assertNotNull(Validators.cardExpiry("13/30", now))
        assertNotNull(Validators.cardExpiry("6/30", now))
    }

    @Test fun cardCvv_isThreeOrFourDigits() {
        assertNull(Validators.cardCvv("123"))
        assertNull(Validators.cardCvv("1234"))
        assertNotNull(Validators.cardCvv("12"))
        assertNotNull(Validators.cardCvv("12a"))
    }

    @Test fun upi_requiresHandle() {
        assertNull(Validators.upiId("reader@okbank"))
        assertNotNull(Validators.upiId("reader"))
        assertNotNull(Validators.upiId(""))
    }
}

class FormattersTest {
    @Test fun price_usesIndianGrouping() {
        assertEquals("₹399", 399L.asPrice())
        assertEquals("₹1,299", 1299L.asPrice())
        assertEquals("₹1,23,456", 123456L.asPrice())
    }
}

class DeliveryEstimatorTest {
    @Test fun skipsWeekends() {
        // Friday 2025-01-03 + 1 business day = Monday 2025-01-06
        assertEquals(LocalDate.of(2025, 1, 6), DeliveryEstimator.estimate(LocalDate.of(2025, 1, 3), 1))
        // Wednesday + 3 business days = next Monday
        assertEquals(LocalDate.of(2025, 1, 13), DeliveryEstimator.estimate(LocalDate.of(2025, 1, 8), 3))
    }

    @Test fun zeroDaysReturnsSameDate() {
        val day = LocalDate.of(2025, 1, 4)
        assertEquals(day, DeliveryEstimator.estimate(day, 0))
    }

    @Test fun neverLandsOnWeekend() {
        val start = LocalDate.of(2025, 1, 1)
        for (offset in 0L..13L) {
            for (days in 1..7) {
                val result = DeliveryEstimator.estimate(start.plusDays(offset), days)
                assertFalse(result.dayOfWeek.value >= 6)
            }
        }
    }
}

class PasswordHasherTest {
    @Test fun verifyAcceptsCorrectPasswordOnly() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("Books1234", salt)
        assertTrue(PasswordHasher.verify("Books1234", salt, hash))
        assertFalse(PasswordHasher.verify("books1234", salt, hash))
    }

    @Test fun saltsDifferAndHashIsNotPlaintext() {
        val a = PasswordHasher.newSalt()
        val b = PasswordHasher.newSalt()
        assertNotEquals(a, b)
        assertNotEquals(PasswordHasher.hash("Books1234", a), PasswordHasher.hash("Books1234", b))
        assertNotEquals("Books1234", PasswordHasher.hash("Books1234", a))
    }
}
