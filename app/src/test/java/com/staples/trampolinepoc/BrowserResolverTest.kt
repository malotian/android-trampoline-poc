package com.staples.trampolinepoc

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
class BrowserResolverTest {

    private lateinit var shadowPackageManager: ShadowPackageManager
    private lateinit var browserResolver: BrowserResolver

    @Before
    fun setUp() {
        val context = Robolectric.buildActivity(TrampolineActivity::class.java).get()
        shadowPackageManager = shadowOf(context.packageManager)
        browserResolver = BrowserResolver(context)
    }

    private fun setupBrowser(packageName: String, supportsCustomTabs: Boolean = false) {
        val intent = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
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
    fun `resolves default browser package`() {
        setupBrowser("com.android.chrome")
        assertEquals("com.android.chrome", browserResolver.resolveDefaultBrowserPackage())
    }

    @Test
    fun `resolves custom tabs package prioritizing default browser`() {
        setupBrowser("com.default.browser", supportsCustomTabs = true)
        setupBrowser("com.secondary.browser", supportsCustomTabs = true)

        assertEquals("com.default.browser", browserResolver.resolveCustomTabsPackage())
    }

    @Test
    fun `resolves custom tabs package falling back to secondary browser`() {
        setupBrowser("com.default.browser", supportsCustomTabs = false)
        setupBrowser("com.secondary.browser", supportsCustomTabs = true)

        assertEquals("com.secondary.browser", browserResolver.resolveCustomTabsPackage())
    }

    @Test
    fun `returns null if no browser supports custom tabs`() {
        setupBrowser("com.default.browser", supportsCustomTabs = false)
        assertNull(browserResolver.resolveCustomTabsPackage())
    }
}
