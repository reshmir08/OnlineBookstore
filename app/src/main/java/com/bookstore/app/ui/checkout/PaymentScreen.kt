package com.bookstore.app.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookstore.app.core.util.asDeliveryDate
import com.bookstore.app.core.util.asPrice
import com.bookstore.app.data.local.AddressEntity
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CartItemEntity
import com.bookstore.app.data.local.CartLine
import com.bookstore.app.data.local.PaymentMethod
import com.bookstore.app.data.payment.SimulatedPaymentGateway
import com.bookstore.app.ui.common.AppSnackbarHost
import com.bookstore.app.ui.common.AppUi
import com.bookstore.app.ui.common.LocalAppUi
import com.bookstore.app.ui.common.BackTopBar
import com.bookstore.app.ui.common.CollectMessages
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.components.FormField
import com.bookstore.app.ui.components.InfoRow
import com.bookstore.app.ui.components.LoadingView
import com.bookstore.app.ui.components.MessageView
import com.bookstore.app.ui.theme.BookstoreTheme
import java.time.LocalDate

/** Checkout step 2: review the order, choose a payment method and pay. */
@Composable
fun PaymentScreen(
    addressId: Long,
    onBack: () -> Unit,
    onChangeAddress: () -> Unit,
    onPaid: (String) -> Unit
) {
    val vm = appViewModel { PaymentViewModel(addressId, it.addressRepository, it.cartRepository, it.orderRepository) }
    CollectMessages(vm.messages)
    LaunchedEffect(vm.completedOrderId) { vm.completedOrderId?.let(onPaid) }

    PaymentContent(
        state = PaymentUiState(
            loading = vm.loading, address = vm.address, lines = vm.lines, deliveryDate = vm.deliveryDate,
            method = vm.method, cardNumber = vm.cardNumber, cardHolder = vm.cardHolder, expiry = vm.expiry,
            cvv = vm.cvv, upiId = vm.upiId, bank = vm.bank, errors = vm.errors, processing = vm.processing,
            failureReason = vm.failureReason, completedOrderId = vm.completedOrderId
        ),
        actions = PaymentActions(
            onBack = onBack, onChangeAddress = onChangeAddress, onPay = vm::pay, onMethod = vm::onMethod,
            onCardNumber = vm::onCardNumber, onCardHolder = vm::onCardHolder, onExpiry = vm::onExpiry,
            onCvv = vm::onCvv, onUpi = vm::onUpi, onBank = vm::onBank, onDismissFailure = vm::dismissFailure
        )
    )
}

private data class PaymentUiState(
    val loading: Boolean = false,
    val address: AddressEntity? = null,
    val lines: List<CartLine> = emptyList(),
    val deliveryDate: LocalDate? = null,
    val method: PaymentMethod = PaymentMethod.CARD,
    val cardNumber: String = "",
    val cardHolder: String = "",
    val expiry: String = "",
    val cvv: String = "",
    val upiId: String = "",
    val bank: String = SimulatedPaymentGateway.BANKS.first(),
    val errors: Map<String, String> = emptyMap(),
    val processing: Boolean = false,
    val failureReason: String? = null,
    val completedOrderId: String? = null
) {
    val total: Long get() = lines.sumOf { it.lineTotal }
}

private class PaymentActions(
    val onBack: () -> Unit = {},
    val onChangeAddress: () -> Unit = {},
    val onPay: () -> Unit = {},
    val onMethod: (PaymentMethod) -> Unit = {},
    val onCardNumber: (String) -> Unit = {},
    val onCardHolder: (String) -> Unit = {},
    val onExpiry: (String) -> Unit = {},
    val onCvv: (String) -> Unit = {},
    val onUpi: (String) -> Unit = {},
    val onBank: (String) -> Unit = {},
    val onDismissFailure: () -> Unit = {}
)

@Composable
private fun PaymentContent(state: PaymentUiState, actions: PaymentActions) {
    Scaffold(
        topBar = { BackTopBar("Payment", actions.onBack) },
        snackbarHost = { AppSnackbarHost() },
        bottomBar = {
            if (!state.loading && state.address != null && state.lines.isNotEmpty()) {
                Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                    Button(
                        onClick = actions.onPay,
                        enabled = !state.processing && state.completedOrderId == null,
                        modifier = Modifier.navigationBarsPadding().fillMaxWidth().padding(16.dp).height(52.dp)
                    ) {
                        if (state.processing) {
                            CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(12.dp))
                            Text("Processing payment…")
                        } else Text("Pay ${state.total.asPrice()}")
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            state.address == null || state.lines.isEmpty() -> MessageView(
                "Nothing to pay for", "Your basket or delivery address is no longer available.",
                Icons.Filled.ShoppingBasket, Modifier.padding(padding), "Go back", actions.onBack
            )
            else -> Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OrderSummary(state, actions.onChangeAddress)
                    PaymentMethods(state, actions)
                }
            }
        }
    }

    state.failureReason?.let { reason ->
        AlertDialog(
            onDismissRequest = actions.onDismissFailure,
            icon = { Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Payment unsuccessful") },
            text = { Text("$reason. You have not been charged and your basket is unchanged. Please try again or use a different payment method.") },
            confirmButton = { TextButton(onClick = actions.onDismissFailure) { Text("Try again") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentScreenPreview() {
    val book = BookEntity(
        id = 1, title = "The Silent Library", author = "A. Writer", brand = "Penguin", categoryId = 1,
        description = "A quiet mystery.", price = 49900, stock = 10, deliveryDays = 3,
        coverStart = 0xFF3B82D4, coverEnd = 0xFF7C5CD8, popularity = 5
    )
    val lines = listOf(
        CartLine(CartItemEntity(userId = 1, bookId = 1, quantity = 2), book),
        CartLine(CartItemEntity(userId = 1, bookId = 2, quantity = 1), book.copy(id = 2, title = "Tales of Kerala", price = 29900))
    )
    val address = AddressEntity(
        id = 1, userId = 1, label = "Home", fullName = "Asha Nair", phone = "9876543210",
        line1 = "12 MG Road", line2 = "Near City Park", city = "Kochi", state = "Kerala",
        postalCode = "682001", isDefault = true
    )
    CompositionLocalProvider(
        LocalAppUi provides AppUi(remember { SnackbarHostState() }, openBasket = {}, logout = {})
    ) {
        BookstoreTheme {
            PaymentContent(
                state = PaymentUiState(
                    address = address, lines = lines, deliveryDate = LocalDate.now().plusDays(3),
                    cardNumber = "4242 4242 4242 4242", cardHolder = "Asha Nair", expiry = "12/30"
                ),
                actions = PaymentActions()
            )
        }
    }
}

@Composable
private fun OrderSummary(vm: PaymentUiState, onChangeAddress: () -> Unit) {
    val address = vm.address ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Order summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            vm.lines.forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${line.book.title} × ${line.item.quantity}", Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(line.lineTotal.asPrice())
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            InfoRow("Delivery", "Free")
            vm.deliveryDate?.let { InfoRow("Tentative delivery", it.asDeliveryDate()) }
            InfoRow("Total payable", vm.total.asPrice(), emphasised = true)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Delivering to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${address.fullName} · ${address.label}", fontWeight = FontWeight.SemiBold)
            Text(address.formatted())
            Text("Phone: ${address.phone}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onChangeAddress, enabled = !vm.processing) { Text("Change address") }
        }
    }
}

@Composable
private fun PaymentMethods(vm: PaymentUiState, actions: PaymentActions) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Payment method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(Modifier.selectableGroup()) {
                PaymentMethod.entries.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = vm.method == m, enabled = !vm.processing, role = Role.RadioButton) { actions.onMethod(m) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = vm.method == m, onClick = null, enabled = !vm.processing)
                        Spacer(Modifier.width(12.dp))
                        Text(m.label)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val enabled = !vm.processing
            when (vm.method) {
                PaymentMethod.CARD -> {
                    FormField(vm.cardNumber, actions.onCardNumber, "Card number", error = vm.errors["card"], keyboardType = KeyboardType.Number, enabled = enabled)
                    FormField(vm.cardHolder, actions.onCardHolder, "Name on card", error = vm.errors["holder"], enabled = enabled)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormField(vm.expiry, actions.onExpiry, "Expiry (MM/YY)", Modifier.weight(1f), error = vm.errors["expiry"], keyboardType = KeyboardType.Number, enabled = enabled)
                        FormField(vm.cvv, actions.onCvv, "CVV", Modifier.weight(1f), error = vm.errors["cvv"], keyboardType = KeyboardType.NumberPassword, password = true, enabled = enabled)
                    }
                    Text(
                        "Demo: any valid card works (e.g. 4242 4242 4242 4242). Use 4000 0000 0000 0002 to see a declined payment.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline
                    )
                }
                PaymentMethod.UPI -> {
                    FormField(vm.upiId, actions.onUpi, "UPI ID (name@bank)", error = vm.errors["upi"], keyboardType = KeyboardType.Email, enabled = enabled)
                    Text(
                        "Demo: any UPI ID works. IDs starting with \"fail\" are rejected.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline
                    )
                }
                PaymentMethod.NET_BANKING -> {
                    BankPicker(vm.bank, enabled, actions.onBank)
                    Text(
                        "Demo: \"${SimulatedPaymentGateway.OFFLINE_BANK}\" simulates an unavailable bank.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun BankPicker(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(selected, Modifier.weight(1f))
            Icon(Icons.Filled.ExpandMore, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SimulatedPaymentGateway.BANKS.forEach { bank ->
                DropdownMenuItem(text = { Text(bank) }, onClick = { onSelect(bank); open = false })
            }
        }
    }
}
