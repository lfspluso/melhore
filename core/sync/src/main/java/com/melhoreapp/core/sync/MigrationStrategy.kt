package com.melhoreapp.core.sync

/**
 * User choice for first-time sign-in data migration (Sprint 19).
 */
sealed class MigrationStrategy {
    /** Upload all local data to this account's cloud. */
    data object UploadLocal : MigrationStrategy()

    /** Download cloud, merge (cloud wins), then upload. */
    data object MergeWithCloud : MigrationStrategy()

    /** Clear local data and use cloud only. */
    data object StartFresh : MigrationStrategy()
}
