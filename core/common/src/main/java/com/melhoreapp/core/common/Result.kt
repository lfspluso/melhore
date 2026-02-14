package com.melhoreapp.core.common

/**
 * Sealed class representing the result of an operation.
 * Used across the app for consistent error/success handling.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
