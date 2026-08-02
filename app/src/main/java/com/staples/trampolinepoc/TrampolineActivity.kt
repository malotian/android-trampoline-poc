package com.staples.trampolinepoc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

/**
 * The "Traffic Cop" that classifies and routes incoming Intents.
 */
class TrampolineActivity : Activity() {

    private lateinit var browserResolver: BrowserResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Data URI: ${intent?.data}")
        browserResolver = BrowserResolver(this)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: Received new intent: $intent")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: run {
            Log.w(TAG, "Trampoline invoked with no data URI.")
            finish()
            return
        }

        val bucket = PathClassifier.classify(uri)
        val bucketName = bucket::class.simpleName ?: "Unknown"
        val caption = intent.getStringExtra(EXTRA_CAPTION) ?: ""
        Log.d(TAG, "Routing URI: $uri → $bucketName")


        val destinationType = when (bucket) {
            is RouteBucket.AppDeepLink -> "Native Screen"
            RouteBucket.Auth -> "Custom Tab Overlay"
            RouteBucket.BrowserOnly -> "System Browser"
            RouteBucket.AuthCallback -> "Auth Success"
            else -> "Browser Fallback"
        }

        Toast.makeText(this, "Middle (Routing): Routing to $destinationType", Toast.LENGTH_SHORT).show()

        when (bucket) {
            is RouteBucket.AppDeepLink -> routeToAppDeepLink(bucket.path, caption)
            RouteBucket.Auth          -> routeToCustomTab(uri)
            RouteBucket.BrowserOnly   -> routeToExplicitBrowser(uri)
            RouteBucket.AuthCallback  -> handleAuthCallback(uri)
            is RouteBucket.Unknown    -> {
                Log.w(TAG, "Unclassified path '${bucket.path}' — falling back to browser.")
                routeToExplicitBrowser(uri)
            }
        }
        finish()
    }

    private fun routeToAppDeepLink(path: String, caption: String? = null) {
        startActivity(
            Intent(this, DeepLinkDestinationActivity::class.java).apply {
                putExtra(DeepLinkDestinationActivity.EXTRA_PATH, path)
                caption?.let { putExtra(DeepLinkDestinationActivity.EXTRA_CAPTION, it) }
            },
        )
    }

    private fun routeToCustomTab(uri: Uri) {
        val ccTabsPackage = browserResolver.resolveCustomTabsPackage()
        Log.d(TAG, "Custom Tabs package: $ccTabsPackage")

        if (ccTabsPackage == null) {
            routeToExplicitBrowser(uri)
            return
        }

        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .apply {
                intent.setPackage(ccTabsPackage)
                launchUrl(this@TrampolineActivity, uri)
            }
    }

    private fun routeToExplicitBrowser(uri: Uri) {
        val browserPackage = browserResolver.resolveDefaultBrowserPackage()
        Log.d(TAG, "Explicit browser package: $browserPackage")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            browserPackage?.let { setPackage(it) }
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No browser available.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Failed to start browser intent.", e)
        }
    }

    private fun handleAuthCallback(uri: Uri) {
        Log.d(TAG, "Auth callback reached app: $uri")
    }

    companion object {
        private const val TAG = "TrampolineActivity"
        const val EXTRA_CAPTION = "extra_caption"
    }
}
