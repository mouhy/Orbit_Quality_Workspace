package com.orbit.mobile.core.network

import com.orbit.mobile.BuildConfig
import com.orbit.mobile.core.datastore.SessionManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

// Auth headers
@Singleton
class AuthInterceptor @Inject constructor(
    private val session: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        session.token?.let { builder.header("Authorization", "Bearer $it") }
        session.userId?.let { builder.header("X-User-Id", it) }
        return chain.proceed(builder.build())
    }
}

// Url rewrite
@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val provider: BaseUrlProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val current = provider.baseUrl
        val default = BuildConfig.BASE_URL
        if (current == default) return chain.proceed(request)

        val url = request.url.toString()
        if (!url.startsWith(default)) return chain.proceed(request)

        val rewritten = (current + url.removePrefix(default)).toHttpUrlOrNull()
            ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().url(rewritten).build())
    }
}
