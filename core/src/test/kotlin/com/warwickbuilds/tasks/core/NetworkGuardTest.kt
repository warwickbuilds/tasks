package com.warwickbuilds.tasks.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tasks makes exactly one kind of network request: the manual "Check for updates" GET to
 * GitHub's releases API, in lib/UpdateChecker.kt. This test fails if network code appears
 * anywhere else in the app's sources.
 */
class NetworkGuardTest {
    private val allowed = "UpdateChecker.kt"
    private val networkCode = Regex(
        """HttpURLConnection|HttpsURLConnection|openConnection\(|\bURL\(|Socket\(|WebView|DownloadManager|""" +
            """import\s+[\w.]*(okhttp|retrofit|ktor|firebase|crashlytics|analytics|gms)""",
        RegexOption.IGNORE_CASE,
    )

    private fun appSources(): List<File> {
        val root = File(System.getProperty("tasks.appSources") ?: "../app/src/main")
        return root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.toList()
    }

    @Test fun onlyTheUpdateCheckerTouchesTheNetwork() {
        val sources = appSources()
        if (sources.isEmpty()) return // core-only checkout
        val offenders = sources.filter { it.name != allowed && networkCode.containsMatchIn(it.readText()) }
        assertEquals("Network code outside $allowed: ${offenders.map { it.name }}", emptyList<File>(), offenders)
    }

    @Test fun theUpdateCheckerOnlyTalksToGitHub() {
        val checker = appSources().firstOrNull { it.name == allowed } ?: return
        val hosts = Regex("""https?://([^/"$]+)""").findAll(checker.readText()).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("api.github.com"), hosts)
    }

    @Test fun manifestHasNoExtraPermissions() {
        val manifest = File(System.getProperty("tasks.appSources") ?: "../app/src/main", "AndroidManifest.xml")
        if (!manifest.exists()) return
        val perms = Regex("""uses-permission[^>]*android:name="([^"]+)"""").findAll(manifest.readText()).map { it.groupValues[1] }.toSet()
        assertTrue("Unexpected permissions: $perms", perms.all { it == "android.permission.INTERNET" })
    }
}
