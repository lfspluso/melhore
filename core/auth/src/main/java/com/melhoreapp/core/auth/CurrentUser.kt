package com.melhoreapp.core.auth

/**
 * Represents the currently signed-in user.
 * @param userId Firebase Auth UID
 * @param email User email (may be null for some providers)
 */
data class CurrentUser(
    val userId: String,
    val email: String?
)
