package com.staples.trampolinepoc

import android.net.Uri

sealed class RouteBucket {
    data class AppDeepLink(val path: String) : RouteBucket()
    object Auth : RouteBucket()
    object BrowserOnly : RouteBucket()
    object AuthCallback : RouteBucket()
    data class Unknown(val path: String) : RouteBucket()
}

data class RoutingConfig(
    val authPrefixes: List<String>,
    val browserOnlyPrefixes: List<String>,
    val appDeepLinkPrefixes: List<String>,
    val callbackScheme: String = "com.staples.trampolinepoc",
    val callbackHost: String = "callback",
)

object PathClassifier {

    val DEFAULT_CONFIG = RoutingConfig(
        authPrefixes = listOf("/login", "/signin", "/sign-in", "/auth/", "/idm/api/identityProxy/sdc/login"),
        browserOnlyPrefixes = listOf("/legal/", "/unsubscribe", "/accessibility", "/terms", "/privacy-policy", "/lp/easyrewardsoverview"),
        appDeepLinkPrefixes = listOf("/p/", "/c/", "/deals/", "/s/", "/product/"),
    )

    fun classify(uri: Uri, config: RoutingConfig = DEFAULT_CONFIG): RouteBucket {
        if ((uri.scheme == config.callbackScheme) && (uri.host == config.callbackHost)) {
            return RouteBucket.AuthCallback
        }

        val path = uri.path ?: ""
        return when {
            config.authPrefixes.any { path.startsWith(it) } -> RouteBucket.Auth
            config.browserOnlyPrefixes.any { path.startsWith(it) } -> RouteBucket.BrowserOnly
            config.appDeepLinkPrefixes.any { path.startsWith(it) } -> RouteBucket.AppDeepLink(path)
            else -> RouteBucket.Unknown(path)
        }
    }
}
