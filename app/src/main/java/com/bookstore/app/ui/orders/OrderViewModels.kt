package com.bookstore.app.ui.orders

import androidx.lifecycle.viewModelScope
import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.local.OrderWithItems
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.OrderRepository
import com.bookstore.app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class OrdersState(val loading: Boolean = true, val orders: List<OrderWithItems> = emptyList())

class OrdersViewModel(orders: OrderRepository, private val cart: CartRepository) : BaseViewModel() {
    val state: StateFlow<OrdersState> = orders.orders()
        .map { OrdersState(loading = false, orders = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersState())

    fun buyAgain(order: OrderWithItems) {
        viewModelScope.launch {
            when (val result = cart.buyAgain(order.items)) {
                is AppResult.Success -> say("Added ${result.value} item(s) to your basket", viewBasket = true)
                is AppResult.Failure -> say(result.message)
            }
        }
    }
}

/** Loads one order. Exposes [now] so the 48-hour eligibility is re-evaluated whenever the screen is opened. */
class OrderDetailViewModel(
    private val orderId: String,
    private val orders: OrderRepository,
    private val cart: CartRepository
) : BaseViewModel() {
    val order: StateFlow<OrderLoad> = orders.order(orderId)
        .map { if (it == null) OrderLoad.Missing else OrderLoad.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderLoad.Loading)

    var cancelling by mutableStateOf(false); private set
    var cancelled by mutableStateOf(false); private set

    fun now(): Long = orders.nowMillis()

    fun cancel() {
        if (cancelling) return
        cancelling = true
        viewModelScope.launch {
            when (val result = orders.cancel(orderId)) {
                is AppResult.Success -> { cancelled = true; say("Order cancelled") }
                is AppResult.Failure -> say(result.message)
            }
            cancelling = false
        }
    }

    fun dismissCancelled() { cancelled = false }

    fun buyAgain(order: OrderWithItems) {
        viewModelScope.launch {
            when (val result = cart.buyAgain(order.items)) {
                is AppResult.Success -> say("Added ${result.value} item(s) to your basket", viewBasket = true)
                is AppResult.Failure -> say(result.message)
            }
        }
    }
}

sealed interface OrderLoad {
    data object Loading : OrderLoad
    data object Missing : OrderLoad
    data class Loaded(val order: OrderWithItems) : OrderLoad
}
