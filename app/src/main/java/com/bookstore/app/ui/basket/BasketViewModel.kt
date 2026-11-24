package com.bookstore.app.ui.basket

import androidx.lifecycle.viewModelScope
import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.local.CartLine
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BasketState(
    val loading: Boolean = true,
    val lines: List<CartLine> = emptyList()
) {
    val total: Long get() = lines.sumOf { it.lineTotal }
    val itemCount: Int get() = lines.sumOf { it.item.quantity }
    val hasUnavailable: Boolean get() = lines.any { it.book.stock < it.item.quantity }
}

class BasketViewModel(private val cart: CartRepository) : BaseViewModel() {
    val state: StateFlow<BasketState> = cart.lines()
        .map { BasketState(loading = false, lines = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BasketState())

    fun increase(line: CartLine) = setQuantity(line, line.item.quantity + 1)
    fun decrease(line: CartLine) = setQuantity(line, line.item.quantity - 1)

    private fun setQuantity(line: CartLine, quantity: Int) {
        viewModelScope.launch {
            val result = cart.setQuantity(line.book.id, quantity)
            if (result is AppResult.Failure) say(result.message)
        }
    }

    fun remove(line: CartLine) {
        viewModelScope.launch {
            cart.remove(line.book.id)
            say("Removed \"${line.book.title}\"")
        }
    }
}
