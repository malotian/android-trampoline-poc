package com.staples.trampolinepoc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri

/**
 * Encapsulates the logic for finding the best available browser for the pattern.
 */
class BrowserResolver(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    fun resolveDefaultBrowserPackage(): String? {
        val queryIntent = Intent(Intent.ACTION_VIEW, "http://example.com".toUri())
        val resolveInfo = packageManager.resolveActivity(queryIntent, 0)
        val packageName = resolveInfo?.activityInfo?.packageName

        if (packageName == context.packageName) {
            return packageManager.queryIntentActivities(queryIntent, PackageManager.MATCH_ALL)
                .asSequence()
                .map { it.activityInfo.packageName }
                .firstOrNull { it != context.packageName }
        }
        return packageName
    }

    fun resolveCustomTabsPackage(): String? {
        val defaultBrowser = resolveDefaultBrowserPackage()
        if ((defaultBrowser != null) && isCustomTabsServiceAvailable(defaultBrowser)) {
            return defaultBrowser
        }

        val queryIntent = Intent(Intent.ACTION_VIEW, "http://example.com".toUri())
        return packageManager.queryIntentActivities(queryIntent, 0)
            .asSequence()
            .map { it.activityInfo.packageName }
            .firstOrNull { isCustomTabsServiceAvailable(it) }
    }

    private fun isCustomTabsServiceAvailable(packageName: String): Boolean {
        val serviceIntent = Intent("android.support.customtabs.action.CustomTabsService").apply {
            setPackage(packageName)
        }
        return packageManager.resolveService(serviceIntent, 0) != null
    }
}
