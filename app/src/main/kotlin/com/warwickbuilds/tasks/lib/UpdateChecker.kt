package com.warwickbuilds.tasks.lib

import com.warwickbuilds.tasks.BuildConfig
import com.warwickbuilds.tasks.core.AvailableUpdate
import com.warwickbuilds.tasks.core.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manual-only "Check for updates" for sideloaded installs.
 *
 * Tasks never touches the network on its own. Only when you tap "Check for updates" in
 * Settings does this make one unauthenticated GET to GitHub's public latest-release
 * endpoint for this repo. No tasks, account, token or device identifier is ever sent.
 * This is the only network code in the app (NetworkGuardTest in :core enforces that).
 */
object UpdateChecker {
    sealed interface Result {
        data class Update(val update: AvailableUpdate) : Result
        data object Current : Result
        data object Unavailable : Result
    }

    val repo: String = BuildConfig.RELEASES_REPO
    val installedVersion: String = BuildConfig.VERSION_NAME

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                useCaches = false
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Tasks-Android")
            }
            if (connection.responseCode != 200) return@withContext Result.Unavailable
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = Updates.parseRelease(body) ?: return@withContext Result.Unavailable
            Updates.availableUpdate(installedVersion, release, repo)?.let { Result.Update(it) } ?: Result.Current
        } catch (e: Exception) {
            Result.Unavailable
        } finally {
            connection?.disconnect()
        }
    }
}
