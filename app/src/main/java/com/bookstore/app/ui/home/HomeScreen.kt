package com.bookstore.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstore.app.core.util.asDateTime
import com.bookstore.app.core.util.asPrice
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.local.OrderItemEntity
import com.bookstore.app.data.local.OrderWithItems
import com.bookstore.app.ui.common.AppSnackbarHost
import com.bookstore.app.ui.common.CollectMessages
import com.bookstore.app.ui.common.TabTopBar
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.components.BookCard
import com.bookstore.app.ui.components.BookCover
import com.bookstore.app.ui.components.LoadingView
import com.bookstore.app.ui.components.SectionHeader
import com.bookstore.app.ui.components.categoryIcon
import com.bookstore.app.ui.orders.StatusChip

@Composable
fun HomeScreen(
    onOpenCategory: (Long) -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenOrders: () -> Unit,
    onOpenOrder: (String) -> Unit
) {
    val vm = appViewModel {
        HomeViewModel(
            it.authRepository, it.catalogRepository, it.orderRepository,
            it.recommendationRepository, it.cartRepository
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    CollectMessages(vm.messages)

    Scaffold(
        topBar = { TabTopBar("Page Turner Books") },
        snackbarHost = { AppSnackbarHost() }
    ) { padding ->
        if (state.loading) {
            LoadingView(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        if (state.userName.isBlank()) "Hello, reader" else "Hello, ${state.userName}",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                    )
                    Text("What would you like to read today?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Column {
                    SectionHeader("Shop by category", Modifier.padding(horizontal = 16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.categories, key = { it.id }) { CategoryChip(it) { onOpenCategory(it.id) } }
                    }
                }
            }
            if (state.recommendations.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader("Recommended for you", Modifier.padding(horizontal = 16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.recommendations, key = { it.book.id }) { rec ->
                                BookCard(
                                    book = rec.book,
                                    reason = rec.reason,
                                    onClick = { onOpenProduct(rec.book.id) },
                                    onAddToBasket = { vm.addToBasket(rec.book.id) },
                                    modifier = Modifier.width(170.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (state.buyAgain.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader("Buy again", Modifier.padding(horizontal = 16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.buyAgain, key = { it.bookId }) { item ->
                                BuyAgainCard(item, { onOpenProduct(item.bookId) }, { vm.buyAgain(item) })
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    SectionHeader("Recent orders", Modifier.padding(horizontal = 16.dp), "View all", onOpenOrders)
                    if (state.recentOrders.isEmpty()) {
                        Text(
                            "You haven't placed any orders yet. Pick a category above to get started.",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.recentOrders.forEach { OrderSummaryCard(it) { onOpenOrder(it.order.id) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: CategoryEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            Modifier.width(112.dp).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(categoryIcon(category.iconKey), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                category.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BuyAgainCard(item: OrderItemEntity, onClick: () -> Unit, onBuyAgain: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(260.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(item.title, item.author, item.coverStart, item.coverEnd, Modifier.width(64.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    item.brand, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onBuyAgain,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("Buy again") }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(order: OrderWithItems, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Order ${order.order.id}", fontWeight = FontWeight.SemiBold)
                StatusChip(order.order.status)
            }
            Text(
                order.items.joinToString { it.title },
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.order.placedAt.asDateTime(), style = MaterialTheme.typography.bodySmall)
                Text(order.order.total.asPrice(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
