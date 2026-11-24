package com.bookstore.app.ui.catalogue

import androidx.lifecycle.viewModelScope
import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.CatalogRepository
import com.bookstore.app.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class CatalogueState(
    val loading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long = 0,
    val brands: List<String> = emptyList(),
    val selectedBrand: String? = null,
    val books: List<BookEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogueViewModel(
    initialCategoryId: Long,
    private val catalog: CatalogRepository,
    private val cart: CartRepository
) : BaseViewModel() {

    private val categoryId = MutableStateFlow(initialCategoryId)
    private val brand = MutableStateFlow<String?>(null)

    private val brands = categoryId.flatMapLatest { catalog.brands(it) }
    private val books = combine(categoryId, brand) { c, b -> c to b }
        .flatMapLatest { (c, b) -> catalog.books(c, b) }

    val state: StateFlow<CatalogueState> = combine(
        categoryId, brand, catalog.categories(), brands, books
    ) { cat, selectedBrand, categories, brandList, bookList ->
        CatalogueState(
            loading = false,
            categories = categories,
            selectedCategoryId = cat,
            brands = brandList,
            selectedBrand = selectedBrand,
            books = bookList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogueState())

    fun selectCategory(id: Long) {
        if (id == categoryId.value) return
        brand.value = null // brands are per category
        categoryId.value = id
    }

    fun selectBrand(value: String?) { brand.value = value }

    fun addToBasket(bookId: Long) = addToBasket(cart, bookId)
}
