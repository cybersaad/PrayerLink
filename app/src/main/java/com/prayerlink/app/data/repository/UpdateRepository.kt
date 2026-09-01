package com.prayerlink.app.data.repository

import com.prayerlink.app.config.AppConfig
import com.prayerlink.app.data.model.GitHubRelease
import com.prayerlink.app.data.model.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UpdateRepository @Inject constructor() {

    suspend fun checkForUpdate(currentVersionName: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/${AppConfig.GITHUB_OWNER}/${AppConfig.GITHUB_REPOSITORY}/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateResult.Error("Failed to check for updates. GitHub API returned code $responseCode")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val isDraft = json.optBoolean("draft", false)
            val isPreRelease = json.optBoolean("prerelease", false)
            
            if (isDraft || isPreRelease) {
                return@withContext UpdateResult.UpToDate
            }

            val tagName = json.getString("tag_name")
            val latestVersion = parseVersion(tagName)
            val currentVersion = parseVersion(currentVersionName)

            if (!isVersionGreater(latestVersion, currentVersion)) {
                return@withContext UpdateResult.UpToDate
            }

            val assets = json.optJSONArray("assets") ?: JSONArray()
            var hasApk = false
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    hasApk = true
                    break
                }
            }

            if (!hasApk) {
                return@withContext UpdateResult.Error("No APK is available for this release.")
            }

            val rawBody = json.optString("body", "")
            val body = if (rawBody.isBlank()) "Bug fixes and improvements." else rawBody

            val release = GitHubRelease(
                version = tagName,
                releaseNotes = body,
                releaseUrl = json.getString("html_url"),
                isPreRelease = isPreRelease,
                isDraft = isDraft,
                hasApkAsset = hasApk,
                publishedAt = json.optString("published_at", "")
            )

            UpdateResult.NewUpdateAvailable(release)

        } catch (e: JSONException) {
            UpdateResult.Error("Invalid response from update server.")
        } catch (e: Exception) {
            UpdateResult.Error("Unable to check for updates. Please try again later.")
        }
    }

    /**
     * Parses a version string like "v1.2.0" into a list of integers [1, 2, 0].
     */
    internal fun parseVersion(version: String): List<Int> {
        return version.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.map { it.toInt() }
    }

    /**
     * Returns true if v1 > v2.
     * E.g. [1, 2, 0] > [1, 1, 0] -> true
     */
    internal fun isVersionGreater(v1: List<Int>, v2: List<Int>): Boolean {
        val maxLen = maxOf(v1.size, v2.size)
        for (i in 0 until maxLen) {
            val part1 = v1.getOrElse(i) { 0 }
            val part2 = v2.getOrElse(i) { 0 }
            if (part1 > part2) return true
            if (part1 < part2) return false
        }
        return false // Equal or less
    }
}
