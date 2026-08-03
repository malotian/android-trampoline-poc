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

    // enhanced routing - not used yet
    fun resolveDefaultBrowserPackageEx(): String? {
        // This is the cleanest way to ask Android for the user's primary browser choice
        // without worrying about domain-specific intent filters.
        val selectorIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
        val selectorPackage = packageManager.resolveActivity(selectorIntent, 0)?.activityInfo?.packageName

        // If the OS returns a valid package that is NOT our own, we use it.
        // Checking against 'context.packageName' is the primary defense against infinite loops.
        if (selectorPackage != null && selectorPackage != context.packageName) {
            return selectorPackage
        }

        // STEP 2: Fallback to resolving a generic web intent
        // If the selector above failed (older Android versions or specific device ROMs),
        // we ask the system who would handle a generic "example.com" URL.
        // We use 'example.com' specifically because our app does NOT have intent filters
        // for it, meaning the system will likely return the default web browser.
        val genericIntent = Intent(Intent.ACTION_VIEW, "http://example.com".toUri())
        val genericPackage = packageManager.resolveActivity(genericIntent, 0)?.activityInfo?.packageName

        // Again, ensure the result is not our own application.
        if (genericPackage != null && genericPackage != context.packageName) {
            return genericPackage
        }

        // STEP 3: Manual search and exclusion (The "Last Resort").
        // If the system still thinks we are the best candidate (e.g., the user forced
        // our app to be the handler for ALL web links), we fetch the full list of
        // every app on the device capable of handling a web URI.
        return packageManager.queryIntentActivities(genericIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { it.activityInfo.packageName }
            // Explicitly filter out our own package name from the candidate list.
            .firstOrNull { it != context.packageName }
    }

}

