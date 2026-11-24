package com.bookstore.app.data.payment

import com.bookstore.app.data.local.PaymentMethod
import kotlinx.coroutines.delay
import java.util.UUID

/** What the customer chose to pay with. Raw card data is only held in memory for the call. */
sealed interface PaymentDetails {
    val method: PaymentMethod

    /** Short, non-sensitive description stored on the order, e.g. "Card ending 4242". */
    fun summary(): String

    data class Card(val number: String, val holder: String, val expiry: String, val cvv: String) : PaymentDetails {
        override val method = PaymentMethod.CARD
        private val digits get() = number.filter { it.isDigit() }
        override fun summary() = "Card ending ${digits.takeLast(4)}"
    }

    data class Upi(val upiId: String) : PaymentDetails {
        override val method = PaymentMethod.UPI
        override fun summary() = "UPI ${upiId.trim()}"
    }

    data class NetBanking(val bank: String) : PaymentDetails {
        override val method = PaymentMethod.NET_BANKING
        override fun summary() = bank
    }
}

sealed interface PaymentOutcome {
    data class Approved(val transactionId: String) : PaymentOutcome
    data class Declined(val reason: String) : PaymentOutcome
}

interface PaymentGateway {
    suspend fun charge(amount: Long, details: PaymentDetails): PaymentOutcome
}

/**
 * Stand-in for a real payment provider. No money moves. Outcomes are deterministic so both
 * the success and failure flows can be exercised:
 *  - Card 4000 0000 0000 0002 is declined; any other valid card is approved.
 *  - UPI IDs starting with "fail" are declined.
 *  - The bank "Demo Bank (Offline)" is unavailable.
 */
class SimulatedPaymentGateway(private val latencyMillis: Long = 1_500) : PaymentGateway {
    override suspend fun charge(amount: Long, details: PaymentDetails): PaymentOutcome {
        delay(latencyMillis)
        val declineReason = when (details) {
            is PaymentDetails.Card ->
                if (details.number.filter { it.isDigit() } == DECLINED_CARD) "Your card was declined by the bank" else null
            is PaymentDetails.Upi ->
                if (details.upiId.trim().startsWith("fail", ignoreCase = true)) "The UPI payment was rejected" else null
            is PaymentDetails.NetBanking ->
                if (details.bank == OFFLINE_BANK) "The bank is currently unavailable" else null
        }
        return declineReason?.let { PaymentOutcome.Declined(it) }
            ?: PaymentOutcome.Approved("TXN" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase())
    }

    companion object {
        const val DECLINED_CARD = "4000000000000002"
        const val OFFLINE_BANK = "Demo Bank (Offline)"
        val BANKS = listOf("State Bank of India", "HDFC Bank", "ICICI Bank", "Axis Bank", OFFLINE_BANK)
    }
}
