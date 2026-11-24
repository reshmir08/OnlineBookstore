package com.bookstore.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstore.app.data.auth.AuthRepository
import com.bookstore.app.data.repo.CartRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(cart: CartRepository, private val auth: AuthRepository) : ViewModel() {
    val cartCount: StateFlow<Int> =
        cart.itemCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun logout() = auth.logout()
}
