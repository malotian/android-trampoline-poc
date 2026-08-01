package com.staples.trampolinepoc

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Phase 1 of the test plan: fast, deterministic, no emulator/device needed.
 * Run with: ./gradlew test
 */
@RunWith(RobolectricTestRunner::class)
class PathClassifierTest {

    private fun uri(url: String) = Uri.parse(url)

    // --- App Deep Link bucket ---

    @Test
    fun `product path routes to AppDeepLink with https`() {
        val result = PathClassifier.classify(uri("https://www.staples.com/p/12345"))
        assertTrue(result is RouteBucket.AppDeepLink)
        assertEquals("/p/12345", (result as RouteBucket.AppDeepLink).path)
    }

    @Test
    fun `product path routes to AppDeepLink with http`() {
        val result = PathClassifier.classify(uri("http://www.staples.com/p/12345"))
        assertTrue(result is RouteBucket.AppDeepLink)
        assertEquals("/p/12345", (result as RouteBucket.AppDeepLink).path)
    }

    @Test
    fun `product path routes to AppDeepLink with https and no-www`() {
        val result = PathClassifier.classify(uri("https://staples.com/p/12345"))
        assertTrue(result is RouteBucket.AppDeepLink)
        assertEquals("/p/12345", (result as RouteBucket.AppDeepLink).path)
    }

    @Test
    fun `category path routes to AppDeepLink`() {
        val result = PathClassifier.classify(uri("https://www.staples.com/c/office-supplies"))
        assertTrue(result is RouteBucket.AppDeepLink)
    }

    // --- Auth bucket ---

    @Test
    fun `login path routes to Auth with https`() {
        val result = PathClassifier.classify(uri("https://www.staples.com/login"))
        assertEquals(RouteBucket.Auth, result)
    }

    @Test
    fun `login path routes to Auth with http`() {
        val result = PathClassifier.classify(uri("http://www.staples.com/login"))
        assertEquals(RouteBucket.Auth, result)
    }

    @Test
    fun `login path with query params still routes to Auth`() {
        val result = PathClassifier.classify(uri("https://www.staples.com/login?returnUrl=/p/123"))
        assertEquals(RouteBucket.Auth, result)
    }

    // --- Auth callback bucket (custom scheme, NOT https) ---

    @Test
    fun `custom scheme callback routes to AuthCallback`() {
        val result = PathClassifier.classify(uri("com.staples.trampolinepoc://callback?code=abc123"))
        assertEquals(RouteBucket.AuthCallback, result)
    }

    @Test
    fun `https callback path does NOT get treated as AuthCallback`() {
        // This is the exact bug the addendum warns about: an https callback
        // should never be relied on as the OAuth termination point. Confirm
        // the classifier doesn't accidentally special-case it as Auth or
        // AuthCallback — it should fall through to Unknown, forcing whoever
        // sees this in logs to notice something is misconfigured.
        val result = PathClassifier.classify(uri("https://www.staples.com/callback?code=abc123"))
        assertTrue(result is RouteBucket.Unknown)
    }

    // --- Browser-only bucket ---

    @Test
    fun `unsubscribe path routes to BrowserOnly`() {
        val result = PathClassifier.classify(uri("https://www.staples.com/unsubscribe?id=999"))
        assertEquals(RouteBucket.BrowserOnly, result)
    }

    @Test
    fun `legal path routes to BrowserOnly`() {
        val result = PathClassifier.classify(uri("https://www.staples.com/legal/terms-of-use"))
        assertEquals(RouteBucket.BrowserOnly, result)
    }

    // --- Bounce-loop regression guard ---

    @Test
    fun `unknown path does not match any known bucket (should fall back to browser, not relaunch Trampoline)`() {
        val result = PathClassifier.classify(uri("https://www.staples.com/mystery-new-path"))
        assertTrue(result is RouteBucket.Unknown)
    }

    @Test
    fun `path with no leading data at all does not crash`() {
        val result = PathClassifier.classify(uri("https://www.staples.com"))
        assertTrue(result is RouteBucket.Unknown)
    }
}
