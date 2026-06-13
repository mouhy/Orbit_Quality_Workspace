package com.orbit.mobile.core.network

import com.orbit.mobile.BuildConfig
import com.orbit.mobile.core.datastore.OrbitDataStore
import javax.inject.Inject
import javax.inject.Singleton

// Runtime base-url
@Singleton
class BaseUrlProvider @Inject constructor(
    private val store: OrbitDataStore
) {
    @Volatile
    var baseUrl: String = BuildConfig.BASE_URL
        private set

    // WS root
    val wsBaseUrl: String
        get() = baseUrl
            .removeSuffix("/")
            .removeSuffix("/api/v1")
            .replaceFirst("https", "wss")
            .replaceFirst("http", "ws")

    // Restore override
    suspend fun restore() {
        val stored = store.snapshot()[OrbitDataStore.Keys.BASE_URL]
        if (!stored.isNullOrBlank()) baseUrl = normalize(stored)
    }

    // Apply override
    suspend fun update(url: String?) {
        if (url.isNullOrBlank()) {
            baseUrl = BuildConfig.BASE_URL
            store.setBaseUrl(null)
        } else {
            baseUrl = normalize(url)
            store.setBaseUrl(baseUrl)
        }
    }

    // Ensure slash
    private fun normalize(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
