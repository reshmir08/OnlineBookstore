package com.bookstore.app.ui.basket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstore.app.core.util.asPrice
import com.bookstore.app.data.local.CartLine
import com.bookstore.app.ui.common.AppSnackbarHost
import com.bookstore.app.ui.common.CollectMessages
import com.bookstore.app.ui.common.TabTopBar
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.components.BookCover
import com.bookstore.app.ui.components.LoadingView
import com.bookstore.app.ui.components.MessageView
import com.bookstore.app.ui.components.QuantityStepper

@Composable
fun BasketScreen(onCheckout: () -> Unit, onBrowse: () -> Unit, onOpenProduct: (Long) -> Unit) {
    val vm = appViewModel { BasketViewModel(it.cartRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    CollectMessages(vm.messages)

    Scaffold(
        topBar = { TabTopBar("Your basket") },
        snackbarHost = { AppSnackbarHost() },
        bottomBar = {
            if (state.lines.isNotEmpty()) CheckoutBar(state, onCheckout)
        }
    ) { padding ->
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            state.lines.isEmpty() -> MessageView(
                "Your basket is empty", "Add some books and they will show up here.",
                Icons.Filled.ShoppingBasket, Modifier.padding(padding), "Browse books", onBrowse
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.lines, key = { it.book.id }) { line ->
                    BasketLineCard(
                        line,
                        onOpen = { onOpenProduct(line.book.id) },
                        onIncrease = { vm.increase(line) },
                        onDecrease = { vm.decrease(line) },
                        onRemove = { vm.remove(line) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BasketLineCard(
    line: CartLine,
    onOpen: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val book = line.book
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp)) {
            BookCover(book, Modifier.width(72.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        book.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onRemove, modifier = Modifier.height(28.dp).width(28.dp)) {
                        Icon(Icons.Filled.Close, "Remove ${book.title} from basket")
                    }
                }
                Text(book.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${book.price.asPrice()} each", style = MaterialTheme.typography.bodySmall)
                if (book.stock < line.item.quantity) {
                    Text(
                        if (book.stock == 0) "Out of stock — remove to continue" else "Only ${book.stock} left — reduce quantity",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(line.item.quantity, onDecrease, onIncrease)
                    Text(line.lineTotal.asPrice(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CheckoutBar(state: BasketState, onCheckout: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total (${state.itemCount} ${if (state.itemCount == 1) "item" else "items"})", style = MaterialTheme.typography.titleMedium)
                Text(state.total.asPrice(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onCheckout,
                enabled = !state.hasUnavailable,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (state.hasUnavailable) "Fix unavailable items to continue" else "Proceed to checkout") }
        }
    }
}
