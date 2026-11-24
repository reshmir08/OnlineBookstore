package com.bookstore.app.core.util

/** Simple success/failure wrapper returned by repositories so ViewModels can show friendly messages. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val message: String) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.fold(onSuccess: (T) -> R, onFailure: (String) -> R): R = when (this) {
    is AppResult.Success -> onSuccess(value)
    is AppResult.Failure -> onFailure(message)
}
