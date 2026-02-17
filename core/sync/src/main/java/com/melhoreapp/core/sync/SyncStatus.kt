package com.melhoreapp.core.sync

/**
 * Current sync state for UI (Sprint 19).
 */
sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data object Synced : SyncStatus()
    data class Error(val message: String?) : SyncStatus()
}
