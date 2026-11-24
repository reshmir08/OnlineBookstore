package com.bookstore.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bookstore.app.data.seed.DatabaseSeeder
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.components.FormField

@Composable
private fun AuthScaffold(title: String, subtitle: String, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Filled.AutoStories, null, Modifier.height(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Page Turner Books", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Hide password" else "Show password"
        )
    }
}

@Composable
private fun SubmitButton(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        if (loading) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        else Text(label)
    }
}

@Composable
fun LoginScreen(onRegister: () -> Unit) {
    val vm = appViewModel { LoginViewModel(it.authRepository) }
    var showPassword by remember { mutableStateOf(false) }
    AuthScaffold("Welcome back", "Sign in to continue shopping") {
        FormField(vm.email, vm::onEmail, "Email", error = vm.emailError, keyboardType = KeyboardType.Email, enabled = !vm.loading)
        FormField(
            vm.password, vm::onPassword, "Password", error = vm.passwordError, password = true,
            showPassword = showPassword, keyboardType = KeyboardType.Password, enabled = !vm.loading,
            trailing = { PasswordToggle(showPassword) { showPassword = !showPassword } }
        )
        vm.formError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(8.dp))
        SubmitButton("Sign in", vm.loading, vm::submit)
        TextButton(onClick = onRegister, enabled = !vm.loading) { Text("New here? Create an account") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = vm::fillDemoCredentials, enabled = !vm.loading) {
            Text(
                "Use demo account (${DatabaseSeeder.DEMO_EMAIL})",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RegisterScreen(onLogin: () -> Unit) {
    val vm = appViewModel { RegisterViewModel(it.authRepository) }
    var showPassword by remember { mutableStateOf(false) }
    AuthScaffold("Create your account", "Join to track orders and get recommendations") {
        FormField(vm.name, vm::onName, "Full name", error = vm.nameError, enabled = !vm.loading)
        FormField(vm.email, vm::onEmail, "Email", error = vm.emailError, keyboardType = KeyboardType.Email, enabled = !vm.loading)
        FormField(
            vm.password, vm::onPassword, "Password (8+ characters, letters & numbers)", error = vm.passwordError,
            password = true, showPassword = showPassword, keyboardType = KeyboardType.Password, enabled = !vm.loading,
            trailing = { PasswordToggle(showPassword) { showPassword = !showPassword } }
        )
        FormField(
            vm.confirm, vm::onConfirm, "Confirm password", error = vm.confirmError, password = true,
            showPassword = showPassword, keyboardType = KeyboardType.Password, enabled = !vm.loading
        )
        vm.formError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(8.dp))
        SubmitButton("Create account", vm.loading, vm::submit)
        TextButton(onClick = onLogin, enabled = !vm.loading) { Text("Already have an account? Sign in") }
    }
}
