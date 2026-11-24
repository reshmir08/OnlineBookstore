package com.bookstore.app.ui.catalogue

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.ui.common.AppSnackbarHost
import com.bookstore.app.ui.common.BackTopBar
import com.bookstore.app.ui.common.CollectMessages
import com.bookstore.app.ui.common.TabTopBar
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.components.BookCard
import com.bookstore.app.ui.components.LoadingView
import com.bookstore.app.ui.components.MessageView
import com.bookstore.app.ui.components.categoryIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ChevronRight

/** Catalogue tab: pick a category to browse. */
@Composable
fun CategoriesScreen(onOpenCategory: (Long) -> Unit) {
    val vm = appViewModel { CatalogueViewModel(1, it.catalogRepository, it.cartRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TabTopBar("Catalogue") },
        snackbarHost = { AppSnackbarHost() }
    ) { padding ->
        if (state.loading) {
            LoadingView(Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Text("Browse by category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(state.categories, key = { it.id }) { category ->
                    CategoryRow(category) { onOpenCategory(category.id) }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(categoryIcon(category.iconKey), null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(category.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Products in a category, with category switching and publisher/brand filtering. */
@Composable
fun CatalogueScreen(initialCategoryId: Long, onBack: () -> Unit, onOpenProduct: (Long) -> Unit) {
    val vm = appViewModel { CatalogueViewModel(initialCategoryId, it.catalogRepository, it.cartRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    CollectMessages(vm.messages)

    val selectedIndex = state.categories.indexOfFirst { it.id == state.selectedCategoryId }.coerceAtLeast(0)
    val title = state.categories.getOrNull(selectedIndex)?.name ?: "Catalogue"

    Scaffold(
        topBar = { BackTopBar(title, onBack) },
        snackbarHost = { AppSnackbarHost() }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.categories.isNotEmpty()) {
                ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 8.dp) {
                    state.categories.forEach { category ->
                        Tab(
                            selected = category.id == state.selectedCategoryId,
                            onClick = { vm.selectCategory(category.id) },
                            text = { Text(category.name) }
                        )
                    }
                }
            }
            if (state.brands.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedBrand == null,
                            onClick = { vm.selectBrand(null) },
                            label = { Text("All publishers") }
                        )
                    }
                    items(state.brands) { brand ->
                        FilterChip(
                            selected = state.selectedBrand == brand,
                            onClick = { vm.selectBrand(if (state.selectedBrand == brand) null else brand) },
                            label = { Text(brand) }
                        )
                    }
                }
            }
            when {
                state.loading -> LoadingView()
                state.books.isEmpty() -> MessageView(
                    "No books found", "There are no books for this selection yet.", Icons.Filled.SearchOff
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "${state.books.size} ${if (state.books.size == 1) "book" else "books"}" +
                                (state.selectedBrand?.let { " from $it" } ?: ""),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.books, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            onClick = { onOpenProduct(book.id) },
                            onAddToBasket = { vm.addToBasket(book.id) }
                        )
                    }
                }
            }
        }
    }
}
