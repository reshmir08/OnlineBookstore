package com.bookstore.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bookstore.app.AppContainer
import com.bookstore.app.BookstoreApp

/**
 * Creates a ViewModel with dependencies from [AppContainer]. Inside [create], `createSavedStateHandle()`
 * gives access to navigation arguments.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: CreationExtras.(AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as BookstoreApp).container
    return viewModel(factory = viewModelFactory { initializer<VM> { create(container) } })
}
