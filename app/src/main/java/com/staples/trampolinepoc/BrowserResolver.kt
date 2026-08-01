package com.staples.trampolinepoc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri

/**
 * Encapsulates the logic for finding the best available browser for the pattern.
 * Decoupled from Activity so it can be tested with a MockContext/Shadows.
 */
class BrowserResolver(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Resolves the user's actual default browser package.
     */
    fun resolveDefaultBrowserPackage(): String? {
        val queryIntent = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
        val resolveInfo = packageManager.resolveActivity(queryIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val packageName = resolveInfo?.activityInfo?.packageName

        // If the "default" is our own app (because of App Links), we need to find the 
        // ACTUAL browser to avoid a loop.
        if (packageName == context.packageName) {
            val allBrowsers = packageManager.queryIntentActivities(queryIntent, PackageManager.MATCH_ALL)
            return allBrowsers
                .map { it.activityInfo.packageName }
                .firstOrNull { it != context.packageName }
        }

        return packageName
    }

    /**
     * Finds the best browser package that supports Chrome Custom Tabs.
     * Prioritizes the default browser if it supports CCT.
     */
    fun resolveCustomTabsPackage(): String? {
        val defaultBrowser = resolveDefaultBrowserPackage()
        if ((defaultBrowser != null) && isCustomTabsServiceAvailable(defaultBrowser)) {
            return defaultBrowser
        }

        // Default browser doesn't support CCT — check other installed browsers
        val queryIntent = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
        val resolvedActivities = packageManager.queryIntentActivities(queryIntent, 0)
        return resolvedActivities
            .asSequence()
            .map { it.activityInfo.packageName }
            .firstOrNull { isCustomTabsServiceAvailable(it) }
    }

    private fun isCustomTabsServiceAvailable(packageName: String): Boolean {
        val serviceIntent = Intent("android.support.customtabs.action.CustomTabsService")
        serviceIntent.setPackage(packageName)
        return packageManager.resolveService(serviceIntent, 0) != null
    }
}
