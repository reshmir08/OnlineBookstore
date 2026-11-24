package com.bookstore.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.repo.CartRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** One-off message shown in a snackbar. [viewBasket] adds a "View basket" action. */
data class UiMessage(val text: String, val viewBasket: Boolean = false)

abstract class BaseViewModel : ViewModel() {
    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    protected fun say(text: String, viewBasket: Boolean = false) {
        _messages.trySend(UiMessage(text, viewBasket))
    }

    protected fun addToBasket(cart: CartRepository, bookId: Long, quantity: Int = 1) {
        viewModelScope.launch {
            when (val result = cart.add(bookId, quantity)) {
                is AppResult.Success -> say("Added to basket", viewBasket = true)
                is AppResult.Failure -> say(result.message)
            }
        }
    }
}
