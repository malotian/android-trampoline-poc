package com.staples.trampolinepoc

import android.net.Uri

/**
 * The three routing buckets described in the approach doc.
 */
sealed class RouteBucket {
    data class AppDeepLink(val path: String) : RouteBucket()
    object Auth : RouteBucket()
    object BrowserOnly : RouteBucket()
    object AuthCallback : RouteBucket()
    data class Unknown(val path: String) : RouteBucket()
}

/**
 * Configuration for the classifier. In a production app, this would likely be
 * initialized from a Remote Config or API response.
 */
data class RoutingConfig(
    val authPrefixes: List<String>,
    val browserOnlyPrefixes: List<String>,
    val appDeepLinkPrefixes: List<String>,
    val callbackScheme: String = "com.staples.trampolinepoc",
    val callbackHost: String = "callback"
)

/**
 * Deliberately framework-light (only depends on android.net.Uri for parsing)
 * so this can be exercised with fast local/unit tests instead of requiring
 * an emulator for every classification-logic change.
 */
object PathClassifier {

    val DEFAULT_CONFIG = RoutingConfig(
        authPrefixes = listOf("/login", "/signin", "/sign-in", "/auth/", "/idm/api/identityProxy/sdc/login"),
        browserOnlyPrefixes = listOf("/legal/", "/unsubscribe", "/accessibility", "/terms", "/privacy-policy", "/lp/easyrewardsoverview"),
        appDeepLinkPrefixes = listOf("/p/", "/c/", "/deals/", "/s/", "/product/")
    )

    fun classify(uri: Uri, config: RoutingConfig = DEFAULT_CONFIG): RouteBucket {
        // Custom-scheme callback bypasses App Link path matching entirely
        if ((uri.scheme == config.callbackScheme) && (uri.host == config.callbackHost)) {
            return RouteBucket.AuthCallback
        }

        val path = uri.path ?: ""

        if (config.authPrefixes.any { path.startsWith(it) }) {
            return RouteBucket.Auth
        }
        if (config.browserOnlyPrefixes.any { path.startsWith(it) }) {
            return RouteBucket.BrowserOnly
        }
        if (config.appDeepLinkPrefixes.any { path.startsWith(it) }) {
            return RouteBucket.AppDeepLink(path)
        }

        return RouteBucket.Unknown(path)
    }
}
