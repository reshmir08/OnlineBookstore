package com.bookstore.app.ui.catalogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstore.app.core.util.asDeliveryDate
import com.bookstore.app.core.util.asPrice
import com.bookstore.app.core.util.tentativeDelivery
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.ui.common.AppSnackbarHost
import com.bookstore.app.ui.common.BackTopBar
import com.bookstore.app.ui.common.CollectMessages
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.components.BookCard
import com.bookstore.app.ui.components.BookCover
import com.bookstore.app.ui.components.LoadingView
import com.bookstore.app.ui.components.MessageView
import com.bookstore.app.ui.components.SectionHeader
import com.bookstore.app.ui.components.StockLabel

@Composable
fun ProductScreen(bookId: Long, onBack: () -> Unit, onOpenProduct: (Long) -> Unit) {
    val vm = appViewModel { ProductViewModel(bookId, it.catalogRepository, it.cartRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    CollectMessages(vm.messages)
    val book = state.book

    Scaffold(
        topBar = { BackTopBar("Book details", onBack) },
        snackbarHost = { AppSnackbarHost() },
        bottomBar = { if (book != null) AddToBasketBar(book) { vm.addToBasket() } }
    ) { padding ->
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            book == null -> MessageView(
                "Book not found", "This book is no longer available.", Icons.Filled.SearchOff,
                Modifier.padding(padding), "Go back", onBack
            )
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        BookCover(book, Modifier.width(140.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (state.categoryName.isNotBlank()) {
                                AssistChip(onClick = {}, label = { Text(state.categoryName) }, enabled = false)
                            }
                            Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("by ${book.author}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Publisher: ${book.brand}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                book.price.asPrice(), style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                            )
                            StockLabel(book.stock)
                        }
                    }

                    DeliveryCard(book, Modifier.padding(horizontal = 16.dp))

                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("About this book", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(book.description, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (state.related.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        Column {
                            SectionHeader("Related books", Modifier.padding(horizontal = 16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.related, key = { it.id }) { related ->
                                    BookCard(
                                        book = related,
                                        onClick = { onOpenProduct(related.id) },
                                        onAddToBasket = { vm.addToBasket(related.id) },
                                        modifier = Modifier.width(170.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryCard(book: BookEntity, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalShipping, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                if (book.stock > 0) {
                    Text("Tentative delivery", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(book.tentativeDelivery().asDeliveryDate(), fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Currently unavailable", fontWeight = FontWeight.SemiBold)
                    Text("No delivery date until the book is back in stock", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AddToBasketBar(book: BookEntity, onAdd: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Button(
            onClick = onAdd,
            enabled = book.stock > 0,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
        ) {
            Icon(Icons.Filled.ShoppingBasket, null)
            Spacer(Modifier.width(8.dp))
            Text(if (book.stock > 0) "Add to basket · ${book.price.asPrice()}" else "Out of stock")
        }
    }
}
