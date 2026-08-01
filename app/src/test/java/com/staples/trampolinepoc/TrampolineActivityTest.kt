package com.staples.trampolinepoc

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowPackageManager
import androidx.core.net.toUri

@RunWith(RobolectricTestRunner::class)
class TrampolineActivityTest {

    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    fun setUp() {
        val context = Robolectric.buildActivity(TrampolineActivity::class.java).get()
        shadowPackageManager = shadowOf(context.packageManager)
    }

    @Suppress("DEPRECATION")
    private fun setupBrowser(packageName: String, supportsCustomTabs: Boolean = false) {
        // Setup the browser activity
        val intent = Intent(Intent.ACTION_VIEW, "http://example.com".toUri())
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "BrowserActivity"
            }
        }
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo)

        if (supportsCustomTabs) {
            val serviceIntent = Intent("android.support.customtabs.action.CustomTabsService").apply {
                setPackage(packageName)
            }
            val serviceInfo = ResolveInfo().apply {
                serviceInfo = ServiceInfo().apply {
                    this.packageName = packageName
                    name = "CustomTabsService"
                }
            }
            shadowPackageManager.addResolveInfoForIntent(serviceIntent, serviceInfo)
        }
    }

    @Test
    fun `routing product URL starts DeepLinkDestinationActivity`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.staples.com/p/123"))
        val activity = Robolectric.buildActivity(TrampolineActivity::class.java, intent).create().get()
        val shadowActivity: ShadowActivity = shadowOf(activity)

        val nextIntent = shadowActivity.nextStartedActivity
        assertNotNull("Should have started a new activity", nextIntent)
        assertEquals(DeepLinkDestinationActivity::class.java.name, nextIntent.component?.className)
        assertEquals("/p/123", nextIntent.getStringExtra(DeepLinkDestinationActivity.EXTRA_PATH))
    }

    @Test
    fun `routing login URL with CCT support starts Custom Tab`() {
        setupBrowser("com.android.chrome", supportsCustomTabs = true)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.staples.com/login"))
        val activity = Robolectric.buildActivity(TrampolineActivity::class.java, intent).create().get()
        val shadowActivity = shadowOf(activity)

        val nextIntent = shadowActivity.nextStartedActivity
        assertNotNull(nextIntent)
        // CustomTabsIntent uses ACTION_VIEW and sets the package
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals("com.android.chrome", nextIntent.`package`)
        assertEquals("http://www.staples.com/login", nextIntent.data.toString())
    }

    @Test
    fun `routing login URL without CCT support starts plain Browser Intent`() {
        setupBrowser("com.some.simple.browser", supportsCustomTabs = false)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.staples.com/login"))
        val activity = Robolectric.buildActivity(TrampolineActivity::class.java, intent).create().get()
        val shadowActivity = shadowOf(activity)

        val nextIntent = shadowActivity.nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals("com.some.simple.browser", nextIntent.`package`)
    }

    @Test
    fun `routing login URL falls back to secondary browser if default doesn't support CCT`() {
        // Default browser (first in resolve list) doesn't support CCT
        setupBrowser("com.default.browser", supportsCustomTabs = false)
        // Secondary browser supports CCT
        setupBrowser("com.chrome.browser", supportsCustomTabs = true)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.staples.com/login"))
        val activity = Robolectric.buildActivity(TrampolineActivity::class.java, intent).create().get()
        val shadowActivity = shadowOf(activity)

        val nextIntent = shadowActivity.nextStartedActivity
        assertNotNull(nextIntent)
        // Should pick the one that supports CCT
        assertEquals("com.chrome.browser", nextIntent.`package`)
    }

    @Test
    fun `routing unsubscribe URL starts Explicit Browser Intent`() {
        setupBrowser("com.android.chrome")

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.staples.com/unsubscribe"))
        val activity = Robolectric.buildActivity(TrampolineActivity::class.java, intent).create().get()
        val shadowActivity = shadowOf(activity)

        val nextIntent = shadowActivity.nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals("com.android.chrome", nextIntent.`package`)
        assertEquals("http://www.staples.com/unsubscribe", nextIntent.data.toString())
    }

    @Test
    fun `routing unknown URL starts Explicit Browser Intent`() {
        setupBrowser("com.android.chrome")

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.staples.com/mystery"))
        val activity = Robolectric.buildActivity(TrampolineActivity::class.java, intent).create().get()
        val shadowActivity = shadowOf(activity)

        val nextIntent = shadowActivity.nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals("com.android.chrome", nextIntent.`package`)
    }

    @Test
    fun `routing auth callback does not start new activity`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("com.staples.trampolinepoc://callback?code=123"))
        val activity = Robolectric.buildActivity(TrampolineActivity::class.java, intent).create().get()
        val shadowActivity = shadowOf(activity)

        // handleAuthCallback currently just logs and toasts
        assertNull("Callback should not launch a new activity in this POC", shadowActivity.nextStartedActivity)
    }

    @Test
    fun `activity finishes immediately after routing`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.staples.com/p/123"))
        val activity = Robolectric.buildActivity(TrampolineActivity::class.java, intent).create().get()
        
        assertEquals(true, activity.isFinishing)
    }
}
