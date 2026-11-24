package com.bookstore.app.data.payment

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedPaymentGatewayTest {
    private val gateway = SimulatedPaymentGateway(latencyMillis = 0)

    @Test fun validCardIsApprovedWithTransactionId() = runTest {
        val outcome = gateway.charge(500, PaymentDetails.Card("4242 4242 4242 4242", "A B", "12/30", "123"))
        assertTrue(outcome is PaymentOutcome.Approved)
        assertTrue((outcome as PaymentOutcome.Approved).transactionId.startsWith("TXN"))
    }

    @Test fun declinedCardIsDeclined() = runTest {
        val outcome = gateway.charge(500, PaymentDetails.Card("4000 0000 0000 0002", "A B", "12/30", "123"))
        assertTrue(outcome is PaymentOutcome.Declined)
    }

    @Test fun upiStartingWithFailIsRejected() = runTest {
        assertTrue(gateway.charge(1, PaymentDetails.Upi("fail@bank")) is PaymentOutcome.Declined)
        assertTrue(gateway.charge(1, PaymentDetails.Upi("reader@bank")) is PaymentOutcome.Approved)
    }

    @Test fun offlineBankIsDeclined() = runTest {
        assertTrue(gateway.charge(1, PaymentDetails.NetBanking(SimulatedPaymentGateway.OFFLINE_BANK)) is PaymentOutcome.Declined)
        assertTrue(gateway.charge(1, PaymentDetails.NetBanking("HDFC Bank")) is PaymentOutcome.Approved)
    }

    @Test fun summariesNeverExposeFullCardNumber() {
        val summary = PaymentDetails.Card("4242 4242 4242 4242", "A B", "12/30", "123").summary()
        assertEquals("Card ending 4242", summary)
    }
}
