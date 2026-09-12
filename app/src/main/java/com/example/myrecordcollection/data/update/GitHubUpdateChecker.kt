package com.example.myrecordcollection.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AvailableUpdate(
    val versionName: String,
    val downloadUrl: String,
    val releasePageUrl: String,
)

class GitHubUpdateChecker {
    suspend fun findUpdate(currentVersionName: String): AvailableUpdate? =
        withContext(Dispatchers.IO) {
            val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "MyRecordCollection-Android")
            }

            try {
                if (connection.responseCode !in 200..299) return@withContext null
                val release = connection.inputStream.bufferedReader().use {
                    JSONObject(it.readText())
                }
                val latestVersion = release.optString("tag_name").removePrefix("v")
                if (!isNewer(latestVersion, currentVersionName.removePrefix("v"))) {
                    return@withContext null
                }

                val assets = release.optJSONArray("assets") ?: return@withContext null
                val apkUrl = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    ?.optString("browser_download_url")
                    ?.takeIf { it.isNotBlank() }
                    ?: return@withContext null

                AvailableUpdate(
                    versionName = latestVersion,
                    downloadUrl = apkUrl,
                    releasePageUrl = release.optString("html_url"),
                )
            } finally {
                connection.disconnect()
            }
        }

    internal fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = candidate.toVersionParts() ?: return false
        val currentParts = current.toVersionParts() ?: return false
        return candidateParts.zip(currentParts)
            .firstOrNull { (candidatePart, currentPart) -> candidatePart != currentPart }
            ?.let { (candidatePart, currentPart) -> candidatePart > currentPart }
            ?: false
    }

    private fun String.toVersionParts(): List<Int>? {
        val parts = split('.')
        if (parts.size != VERSION_PARTS) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    private companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/VRyzhkov/MyRecordCollection/releases/latest"
        const val TIMEOUT_MILLIS = 10_000
        const val VERSION_PARTS = 3
    }
}
