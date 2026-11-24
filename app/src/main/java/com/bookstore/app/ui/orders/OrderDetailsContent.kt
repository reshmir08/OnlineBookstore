package com.bookstore.app.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookstore.app.core.util.asDateTime
import com.bookstore.app.core.util.asDeliveryDate
import com.bookstore.app.core.util.asPrice
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.local.OrderWithItems
import com.bookstore.app.ui.components.BookCover
import com.bookstore.app.ui.components.InfoRow
import com.bookstore.app.ui.theme.StatusColors
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        OrderStatus.PLACED -> "Confirmed" to StatusColors.inStock
        OrderStatus.CANCELLED -> "Cancelled" to StatusColors.outOfStock
    }
    Surface(modifier, shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.12f)) {
        Text(
            label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold
        )
    }
}

/** Purchased items, quantities, totals, address, payment and delivery details; shared by the confirmation and order-detail screens. */
@Composable
fun OrderDetailsContent(order: OrderWithItems, showPayment: Boolean = true) {
    val o = order.order
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Order ${o.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusChip(o.status)
                }
                InfoRow("Placed on", o.placedAt.asDateTime())
                if (o.status == OrderStatus.CANCELLED && o.cancelledAt != null) {
                    InfoRow("Cancelled on", o.cancelledAt.asDateTime())
                } else {
                    InfoRow("Tentative delivery", LocalDate.ofEpochDay(o.estimatedDeliveryEpochDay).asDeliveryDate())
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                order.items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BookCover(item.title, item.author, item.coverStart, item.coverEnd, Modifier.width(48.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Qty ${item.quantity} × ${item.unitPrice.asPrice()}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text((item.unitPrice * item.quantity).asPrice(), fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider()
                InfoRow("Total amount", o.total.asPrice(), emphasised = true)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Delivery address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.padding(top = 4.dp))
                Text(o.addressName, fontWeight = FontWeight.SemiBold)
                Text(o.addressText)
                Text("Phone: ${o.addressPhone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (showPayment) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    InfoRow("Method", o.paymentMethod.label)
                    InfoRow("Details", o.paymentDetail)
                    InfoRow("Transaction ID", o.transactionId)
                    InfoRow("Paid on", o.paidAt.asDateTime())
                    InfoRow(if (o.status == OrderStatus.CANCELLED) "Amount (refund initiated)" else "Amount paid", o.total.asPrice())
                }
            }
        }
    }
}
