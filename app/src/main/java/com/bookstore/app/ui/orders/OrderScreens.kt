package com.bookstore.app.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.local.OrderWithItems
import com.bookstore.app.domain.CancellationPolicy
import com.bookstore.app.ui.common.AppSnackbarHost
import com.bookstore.app.ui.common.BackTopBar
import com.bookstore.app.ui.common.CollectMessages
import com.bookstore.app.ui.common.TabTopBar
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.components.LoadingView
import com.bookstore.app.ui.components.MessageView
import com.bookstore.app.ui.components.SuccessBanner
import com.bookstore.app.ui.home.OrderSummaryCard

/** Order history with Buy Again. */
@Composable
fun OrdersScreen(onOpenOrder: (String) -> Unit, onBrowse: () -> Unit) {
    val vm = appViewModel { OrdersViewModel(it.orderRepository, it.cartRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    CollectMessages(vm.messages)

    Scaffold(
        topBar = { TabTopBar("Your orders") },
        snackbarHost = { AppSnackbarHost() }
    ) { padding ->
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            state.orders.isEmpty() -> MessageView(
                "No orders yet", "Books you buy will appear here so you can review or buy them again.",
                Icons.AutoMirrored.Filled.ReceiptLong, Modifier.padding(padding), "Start shopping", onBrowse
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(state.orders, key = { it.order.id }) { order ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OrderSummaryCard(order) { onOpenOrder(order.order.id) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onOpenOrder(order.order.id) }) { Text("View details") }
                            Button(
                                onClick = { vm.buyAgain(order) },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) { Text("Buy again") }
                        }
                    }
                }
            }
        }
    }
}

/** Full purchase details for one order, with Buy Again and 48-hour cancellation. */
@Composable
fun OrderDetailScreen(orderId: String, onBack: () -> Unit) {
    val vm = appViewModel { OrderDetailViewModel(orderId, it.orderRepository, it.cartRepository) }
    val load by vm.order.collectAsStateWithLifecycle()
    CollectMessages(vm.messages)
    var confirmCancel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BackTopBar("Order details", onBack) },
        snackbarHost = { AppSnackbarHost() }
    ) { padding ->
        when (val current = load) {
            OrderLoad.Loading -> LoadingView(Modifier.padding(padding))
            OrderLoad.Missing -> MessageView(
                "Order not found", "We couldn't find this order.", Icons.Filled.SearchOff,
                Modifier.padding(padding), "Go back", onBack
            )
            is OrderLoad.Loaded -> {
                val order = current.order
                val now = vm.now()
                val eligible = CancellationPolicy.isEligible(order.order, now)
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OrderDetailsContent(order)
                        CancellationSection(order, eligible, now, vm.cancelling) { confirmCancel = true }
                        OutlinedButton(onClick = { vm.buyAgain(order) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            Text("Buy again")
                        }
                    }
                }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this order?") },
            text = { Text("Order $orderId will be cancelled and your payment refunded. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmCancel = false; vm.cancel() }) {
                    Text("Yes, cancel order", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep order") } }
        )
    }

    if (vm.cancelled) {
        AlertDialog(
            onDismissRequest = vm::dismissCancelled,
            title = { Text("Order cancelled") },
            text = {
                SuccessBanner(
                    "Cancellation confirmed",
                    "Order $orderId has been cancelled. Your refund will be processed to your original payment method."
                )
            },
            confirmButton = { TextButton(onClick = vm::dismissCancelled) { Text("OK") } }
        )
    }
}

@Composable
private fun CancellationSection(
    order: OrderWithItems,
    eligible: Boolean,
    now: Long,
    cancelling: Boolean,
    onCancel: () -> Unit
) {
    val o = order.order
    if (o.status == OrderStatus.CANCELLED) return
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (eligible) {
                Text("Cancellation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "You can cancel this order for free within 48 hours of placing it. " +
                        CancellationPolicy.remainingLabel(o, now) + "."
                )
                Button(
                    onClick = onCancel,
                    enabled = !cancelling,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (cancelling) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                    else Text("Cancel order")
                }
            } else {
                Text("Cancellation unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("The 48-hour cancellation window for this order has ended.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Shown right after payment succeeds: confirmation, payment details and purchase summary. */
@Composable
fun ConfirmationScreen(orderId: String, onViewOrder: (String) -> Unit, onContinueShopping: () -> Unit) {
    val vm = appViewModel { OrderDetailViewModel(orderId, it.orderRepository, it.cartRepository) }
    val load by vm.order.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { androidx.compose.material3.CenterAlignedTopAppBar(title = { Text("Order confirmed") }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onViewOrder(orderId) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("View order details")
                    }
                    OutlinedButton(onClick = onContinueShopping, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Continue shopping")
                    }
                }
            }
        }
    ) { padding ->
        when (val current = load) {
            OrderLoad.Loading -> LoadingView(Modifier.padding(padding))
            OrderLoad.Missing -> MessageView(
                "Order not found", "We couldn't load this order.", Icons.Filled.SearchOff,
                Modifier.padding(padding), "Continue shopping", onContinueShopping
            )
            is OrderLoad.Loaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SuccessBanner(
                        "Thank you for your order!",
                        "Payment successful. Order ID ${current.order.order.id}"
                    )
                    OrderDetailsContent(current.order)
                }
            }
        }
    }
}
