package com.example.ledgerscanner.network

import com.example.ledgerscanner.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val accessToken = tokenStore.getAccessToken()

        val request = if (!accessToken.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
