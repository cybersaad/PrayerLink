package com.prayerlink.app.data.model

data class GitHubRelease(
    val version: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val isPreRelease: Boolean,
    val isDraft: Boolean,
    val hasApkAsset: Boolean,
    val publishedAt: String
)

sealed class UpdateResult {
    data class NewUpdateAvailable(val release: GitHubRelease) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}
