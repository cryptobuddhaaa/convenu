package com.convenu.app.data.api

import com.convenu.app.data.repository.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for public endpoints
        val path = originalRequest.url.encodedPath
        if (path.contains("auth/telegram")) {
            return chain.proceed(originalRequest)
        }

        // Read token from DataStore (runBlocking is acceptable on OkHttp dispatcher threads)
        val token = runBlocking { tokenManager.tokenFlow.firstOrNull() }

        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}
