package com.bookstore.app.ui.checkout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.bookstore.app.core.util.Validators
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.CartLine
import com.bookstore.app.data.local.PaymentMethod
import com.bookstore.app.data.payment.PaymentDetails
import com.bookstore.app.data.payment.SimulatedPaymentGateway
import com.bookstore.app.data.repo.AddressRepository
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.CheckoutResult
import com.bookstore.app.data.repo.OrderRepository
import com.bookstore.app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class PaymentViewModel(
    private val addressId: Long,
    private val addresses: AddressRepository,
    private val cart: CartRepository,
    private val orders: OrderRepository
) : BaseViewModel() {

    // Snapshot taken once, so emptying the basket after payment cannot flicker this screen.
    var loading by mutableStateOf(true); private set
    var address by mutableStateOf<AddressEntity?>(null); private set
    var lines by mutableStateOf<List<CartLine>>(emptyList()); private set
    var deliveryDate by mutableStateOf<LocalDate?>(null); private set
    val total: Long get() = lines.sumOf { it.lineTotal }

    var method by mutableStateOf(PaymentMethod.CARD); private set
    var cardNumber by mutableStateOf(""); private set
    var cardHolder by mutableStateOf(""); private set
    var expiry by mutableStateOf(""); private set
    var cvv by mutableStateOf(""); private set
    var upiId by mutableStateOf(""); private set
    var bank by mutableStateOf(SimulatedPaymentGateway.BANKS.first()); private set

    var errors by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var processing by mutableStateOf(false); private set
    var failureReason by mutableStateOf<String?>(null); private set
    var completedOrderId by mutableStateOf<String?>(null); private set

    init {
        viewModelScope.launch {
            address = addresses.find(addressId)
            lines = cart.lines().first()
            deliveryDate = if (lines.isEmpty()) null else orders.deliveryDateFor(lines)
            loading = false
        }
    }

    private fun clear(field: String) { if (field in errors) errors = errors - field }

    fun onMethod(m: PaymentMethod) { method = m; errors = emptyMap() }
    fun onCardNumber(v: String) {
        val digits = v.filter { it.isDigit() }.take(19)
        cardNumber = digits.chunked(4).joinToString(" "); clear("card")
    }
    fun onCardHolder(v: String) { cardHolder = v; clear("holder") }
    fun onExpiry(v: String) {
        val digits = v.filter { it.isDigit() }.take(4)
        expiry = if (digits.length > 2) digits.substring(0, 2) + "/" + digits.substring(2) else digits
        clear("expiry")
    }
    fun onCvv(v: String) { cvv = v.filter { it.isDigit() }.take(4); clear("cvv") }
    fun onUpi(v: String) { upiId = v; clear("upi") }
    fun onBank(v: String) { bank = v }
    fun dismissFailure() { failureReason = null }

    private fun validate(): Map<String, String> = buildMap {
        when (method) {
            PaymentMethod.CARD -> {
                Validators.cardNumber(cardNumber)?.let { put("card", it) }
                Validators.name(cardHolder)?.let { put("holder", it) }
                Validators.cardExpiry(expiry, YearMonth.now())?.let { put("expiry", it) }
                Validators.cardCvv(cvv)?.let { put("cvv", it) }
            }
            PaymentMethod.UPI -> Validators.upiId(upiId)?.let { put("upi", it) }
            PaymentMethod.NET_BANKING -> Unit
        }
    }

    fun pay() {
        val delivery = address ?: return
        if (processing || completedOrderId != null) return
        errors = validate()
        if (errors.isNotEmpty()) return
        val details = when (method) {
            PaymentMethod.CARD -> PaymentDetails.Card(cardNumber, cardHolder.trim(), expiry, cvv)
            PaymentMethod.UPI -> PaymentDetails.Upi(upiId.trim())
            PaymentMethod.NET_BANKING -> PaymentDetails.NetBanking(bank)
        }
        processing = true
        viewModelScope.launch {
            when (val result = orders.checkout(delivery, details)) {
                is CheckoutResult.Success -> completedOrderId = result.orderId
                is CheckoutResult.PaymentFailed -> failureReason = result.reason
                is CheckoutResult.Error -> say(result.message)
            }
            processing = false
        }
    }
}
