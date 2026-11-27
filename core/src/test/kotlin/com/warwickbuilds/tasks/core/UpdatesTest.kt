package com.warwickbuilds.tasks.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    private val repo = "warwickbuilds/tasks"
    private fun release(tag: String, vararg assets: ReleaseAsset, url: String = "https://github.com/$repo/releases/tag/$tag") =
        ReleaseInfo(tag, url, assets = assets.toList())

    @Test fun parsesAndComparesVersions() {
        assertEquals(Version(1, 2, 3), Version.parse("v1.2.3"))
        assertNull(Version.parse("1.2"))
        assertTrue(Version.parse("0.10.0")!! > Version.parse("0.9.9")!!)
        assertEquals(10203, Version.code("1.2.3"))
        assertEquals(100, Version.code("0.1.0"))
    }

    @Test fun offersNewerReleaseWithApk() {
        val apk = ReleaseAsset("tasks-0.2.0.apk", "https://github.com/$repo/releases/download/v0.2.0/tasks-0.2.0.apk")
        val update = Updates.availableUpdate("0.1.0", release("v0.2.0", apk), repo)!!
        assertEquals("0.2.0", update.version)
        assertEquals(apk.downloadUrl, update.downloadUrl)
    }

    @Test fun nothingWhenCurrentOrOlder() {
        assertNull(Updates.availableUpdate("0.2.0", release("v0.2.0"), repo))
        assertNull(Updates.availableUpdate("0.3.0", release("v0.2.0"), repo))
    }

    @Test fun ignoresDraftsPrereleasesAndStrangeLinks() {
        assertNull(Updates.availableUpdate("0.1.0", release("v0.2.0").copy(prerelease = true), repo))
        assertNull(Updates.availableUpdate("0.1.0", release("v0.2.0", url = "https://evil.example/x"), repo))
        val bad = ReleaseAsset("x.apk", "https://evil.example/x.apk")
        assertEquals("https://github.com/$repo/releases/tag/v0.2.0", Updates.availableUpdate("0.1.0", release("v0.2.0", bad), repo)!!.downloadUrl)
    }

    @Test fun trustedUrls() {
        assertTrue(Updates.isTrustedReleaseUrl("https://github.com/$repo/releases/tag/v1.0.0", repo))
        assertFalse(Updates.isTrustedReleaseUrl("http://github.com/$repo/releases/tag/v1.0.0", repo))
        assertFalse(Updates.isTrustedReleaseUrl("https://github.com/other/repo/releases/tag/v1.0.0", repo))
        assertFalse(Updates.isTrustedReleaseUrl("https://github.com.evil.example/$repo/releases/", repo))
        assertFalse(Updates.isTrustedReleaseUrl("not a url", repo))
    }

    @Test fun parsesGitHubJson() {
        val body = """{"tag_name":"v0.2.0","html_url":"https://github.com/$repo/releases/tag/v0.2.0","draft":false,
            "prerelease":false,"assets":[{"name":"tasks-0.2.0.apk","browser_download_url":"https://github.com/$repo/releases/download/v0.2.0/tasks-0.2.0.apk","size":1}],"body":"x"}"""
        assertEquals("v0.2.0", Updates.parseRelease(body)!!.tagName)
        assertNull(Updates.parseRelease("<html>"))
    }
}
