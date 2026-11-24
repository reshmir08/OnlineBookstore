package com.bookstore.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.bookstore.app.core.util.AppResult
import com.bookstore.app.core.util.Validators
import com.bookstore.app.data.auth.AuthRepository
import com.bookstore.app.ui.common.BaseViewModel
import kotlinx.coroutines.launch

class LoginViewModel(private val auth: AuthRepository) : BaseViewModel() {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var emailError by mutableStateOf<String?>(null)
        private set
    var passwordError by mutableStateOf<String?>(null)
        private set
    var formError by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    fun onEmail(value: String) { email = value; emailError = null; formError = null }
    fun onPassword(value: String) { password = value; passwordError = null; formError = null }

    fun fillDemoCredentials() {
        onEmail(com.bookstore.app.data.seed.DatabaseSeeder.DEMO_EMAIL)
        onPassword(com.bookstore.app.data.seed.DatabaseSeeder.DEMO_PASSWORD)
    }

    fun submit() {
        if (loading) return
        emailError = Validators.email(email)
        passwordError = if (password.isEmpty()) "Password is required" else null
        if (emailError != null || passwordError != null) return
        loading = true
        viewModelScope.launch {
            // On success the session flow flips and navigation moves to Home.
            when (val result = auth.login(email, password)) {
                is AppResult.Failure -> formError = result.message
                is AppResult.Success -> Unit
            }
            loading = false
        }
    }
}

class RegisterViewModel(private val auth: AuthRepository) : BaseViewModel() {
    var name by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var confirm by mutableStateOf("")
        private set
    var nameError by mutableStateOf<String?>(null)
        private set
    var emailError by mutableStateOf<String?>(null)
        private set
    var passwordError by mutableStateOf<String?>(null)
        private set
    var confirmError by mutableStateOf<String?>(null)
        private set
    var formError by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    fun onName(v: String) { name = v; nameError = null }
    fun onEmail(v: String) { email = v; emailError = null; formError = null }
    fun onPassword(v: String) { password = v; passwordError = null }
    fun onConfirm(v: String) { confirm = v; confirmError = null }

    fun submit() {
        if (loading) return
        nameError = Validators.name(name)
        emailError = Validators.email(email)
        passwordError = Validators.password(password)
        confirmError = Validators.confirmPassword(password, confirm)
        if (listOf(nameError, emailError, passwordError, confirmError).any { it != null }) return
        loading = true
        viewModelScope.launch {
            when (val result = auth.register(name, email, password)) {
                is AppResult.Failure -> formError = result.message
                is AppResult.Success -> Unit
            }
            loading = false
        }
    }
}
