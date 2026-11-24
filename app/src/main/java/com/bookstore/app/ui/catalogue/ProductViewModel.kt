package com.bookstore.app.ui.catalogue

import androidx.lifecycle.viewModelScope
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.CatalogRepository
import com.bookstore.app.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductState(
    val loading: Boolean = true,
    val book: BookEntity? = null,
    val categoryName: String = "",
    val related: List<BookEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModel(
    private val bookId: Long,
    private val catalog: CatalogRepository,
    private val cart: CartRepository
) : BaseViewModel() {

    init {
        // Viewing a product is the "browsing preference" signal behind recommendations.
        viewModelScope.launch { catalog.recordView(bookId) }
    }

    val state: StateFlow<ProductState> = catalog.book(bookId).flatMapLatest { book ->
        if (book == null) flowOf(ProductState(loading = false))
        else combine(catalog.related(book), catalog.category(book.categoryId)) { related, category ->
            ProductState(loading = false, book = book, categoryName = category?.name.orEmpty(), related = related)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductState())

    fun addToBasket(id: Long = bookId, quantity: Int = 1) = addToBasket(cart, id, quantity)
}
