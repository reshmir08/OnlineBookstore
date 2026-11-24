package com.bookstore.app.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow

/** App-wide UI services made available to every screen. */
class AppUi(
    val snackbar: SnackbarHostState,
    val openBasket: () -> Unit,
    val logout: () -> Unit
)

val LocalAppUi = compositionLocalOf<AppUi> { error("AppUi not provided") }

@Composable
fun AppSnackbarHost() = SnackbarHost(LocalAppUi.current.snackbar)

/** Shows each [UiMessage] from a ViewModel in the snackbar; "View basket" opens the basket. */
@Composable
fun CollectMessages(messages: Flow<UiMessage>) {
    val ui = LocalAppUi.current
    LaunchedEffect(messages) {
        messages.collect { message ->
            ui.snackbar.currentSnackbarData?.dismiss()
            val result = ui.snackbar.showSnackbar(
                message = message.text,
                actionLabel = if (message.viewBasket) "View basket" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) ui.openBasket()
        }
    }
}

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        },
        actions = actions
    )
}

/** Top bar for the four main tabs, with a logout action guarded by a confirmation dialog. */
@Composable
fun TabTopBar(title: String) {
    val ui = LocalAppUi.current
    var confirm by remember { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = { confirm = true }) { Icon(Icons.AutoMirrored.Filled.Logout, "Log out") }
        }
    )
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Log out?") },
            text = { Text("You will need to sign in again to continue shopping.") },
            confirmButton = { TextButton(onClick = { confirm = false; ui.logout() }) { Text("Log out") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Stay signed in") } }
        )
    }
}
