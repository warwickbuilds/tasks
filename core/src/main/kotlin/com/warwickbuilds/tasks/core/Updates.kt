package com.warwickbuilds.tasks.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

/**
 * Version parsing and GitHub release reading for the manual "Check for updates" button.
 * The network call itself is in the app (lib/UpdateChecker.kt); this part is pure.
 * gradle.properties' appVersion ("0.1.0") is the single source of truth and releases are
 * tagged "v0.1.0".
 */
data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(this, other, Version::major, Version::minor, Version::patch)

    override fun toString() = "$major.$minor.$patch"

    companion object {
        private val pattern = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

        fun parse(text: String?): Version? {
            val m = pattern.find(text?.trim() ?: return null) ?: return null
            return Version(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }

        /** Android versionCode: 1.2.3 -> 10203. */
        fun code(text: String): Int {
            val v = requireNotNull(parse(text)) { "Bad version '$text'. Use major.minor.patch." }
            require(v.minor < 100 && v.patch < 100) { "minor and patch must be under 100" }
            return v.major * 10_000 + v.minor * 100 + v.patch
        }
    }
}

@Serializable
data class ReleaseInfo(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<ReleaseAsset> = emptyList(),
)

@Serializable
data class ReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

data class AvailableUpdate(
    val version: String,
    /** Direct APK link when the release has one, otherwise the release page. */
    val downloadUrl: String,
    val pageUrl: String,
)

object Updates {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseRelease(body: String): ReleaseInfo? = runCatching { json.decodeFromString<ReleaseInfo>(body) }.getOrNull()

    /** Only HTTPS links on github.com, under the expected repository's releases when [repo] is given. */
    fun isTrustedReleaseUrl(url: String?, repo: String? = null): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme != "https" || uri.host != "github.com" || uri.userInfo != null || uri.port != -1) return false
        if (repo == null) return true
        return (uri.path ?: "").lowercase().startsWith("/${repo.lowercase()}/releases/")
    }

    fun pickApk(assets: List<ReleaseAsset>): ReleaseAsset? =
        assets.firstOrNull { it.name.lowercase().endsWith(".apk") }

    /** The update to offer, or null when [installed] is current (or the release looks wrong). */
    fun availableUpdate(installed: String?, release: ReleaseInfo, repo: String?): AvailableUpdate? {
        if (release.draft || release.prerelease) return null
        val latest = Version.parse(release.tagName) ?: return null
        val current = Version.parse(installed) ?: return null
        if (latest <= current) return null
        if (!isTrustedReleaseUrl(release.htmlUrl, repo)) return null
        val apk = pickApk(release.assets)?.downloadUrl
        val download = if (isTrustedReleaseUrl(apk, repo)) apk!! else release.htmlUrl
        return AvailableUpdate(latest.toString(), download, release.htmlUrl)
    }
}
