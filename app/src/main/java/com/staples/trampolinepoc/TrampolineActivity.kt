package com.staples.trampolinepoc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

/**
 * The "Traffic Cop" from the approach doc.
 */
class TrampolineActivity : Activity() {

    private lateinit var browserResolver: BrowserResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started with intent: $intent")
        Log.d(TAG, "Data URI: ${intent?.data}")
        browserResolver = BrowserResolver(this)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: Received new intent: $intent")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri == null) {
            Log.w(TAG, "Trampoline invoked with no data URI — nothing to route.")
            finish()
            return
        }

        Log.d(TAG, "Routing incoming URI: $uri")

        val bucket = PathClassifier.classify(uri)
        Toast.makeText(this, "Classified as: ${bucket::class.simpleName}", Toast.LENGTH_SHORT).show()

        when (bucket) {
            is RouteBucket.AppDeepLink -> routeToAppDeepLink(bucket.path)
            RouteBucket.Auth -> routeToCustomTab(uri)
            RouteBucket.BrowserOnly -> routeToExplicitBrowser(uri)
            RouteBucket.AuthCallback -> handleAuthCallback(uri)
            is RouteBucket.Unknown -> {
                Log.w(TAG, "Unclassified path '${bucket.path}' — falling back to browser.")
                routeToExplicitBrowser(uri)
            }
        }

        // Never leave the Trampoline in the back-stack
        finish()
    }

    private fun routeToAppDeepLink(path: String) {
        val intent = Intent(this, DeepLinkDestinationActivity::class.java).apply {
            putExtra(DeepLinkDestinationActivity.EXTRA_PATH, path)
        }
        startActivity(intent)
    }

    private fun routeToCustomTab(uri: Uri) {
        val ccTabsPackage = browserResolver.resolveCustomTabsPackage()
        Log.d(TAG, "Custom Tabs package resolved to: $ccTabsPackage")

        if (ccTabsPackage == null) {
            Log.w(TAG, "No Custom-Tabs-capable browser found — falling back to plain browser.")
            routeToExplicitBrowser(uri)
            return
        }

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        Log.d(TAG, "Launching Custom Tab for: $uri using package: $ccTabsPackage")
        customTabsIntent.intent.setPackage(ccTabsPackage)
        customTabsIntent.launchUrl(this, uri)
    }

    private fun routeToExplicitBrowser(uri: Uri) {
        val browserPackage = browserResolver.resolveDefaultBrowserPackage()
        Log.d(TAG, "Explicit browser package resolved to: $browserPackage")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (browserPackage != null) {
            intent.setPackage(browserPackage)
        }

        try {
            Log.d(TAG, "Starting explicit browser activity for: $uri")
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "No browser available to open this link.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "No activity found to handle explicit browser intent.", e)
        }
    }

    private fun handleAuthCallback(uri: Uri) {
        Log.d(TAG, "Auth callback received successfully: $uri")
        Toast.makeText(this, "Auth callback reached the app ✅", Toast.LENGTH_LONG).show()
        // Production note: parse token/code from `uri`, complete sign-in,
        // then route the user to wherever they were headed pre-auth.
    }

    companion object {
        private const val TAG = "TrampolineActivity"
    }
}
