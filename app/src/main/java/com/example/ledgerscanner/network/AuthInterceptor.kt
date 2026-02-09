package com.example.ledgerscanner.network

import com.example.ledgerscanner.auth.AuthState
import com.example.ledgerscanner.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val tokenAuthenticator: TokenAuthenticator
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

        val response = chain.proceed(request)
        if (response.code != 403) {
            return response
        }

        // Avoid infinite retry loops.
        if (request.header(HEADER_RETRY) == HEADER_RETRY_VALUE) {
            return response
        }

        val refreshToken = tokenStore.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            response.close()
            tokenStore.clear()
            AuthState.notifyLoggedOut()
            return response
        }

        val newTokens = tokenAuthenticator.refreshTokensBlocking(refreshToken)
        if (newTokens == null) {
            response.close()
            tokenStore.clear()
            AuthState.notifyLoggedOut()
            return response
        }

        tokenStore.saveTokens(newTokens.first, newTokens.second)
        response.close()

        val retried = request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.first}")
            .header(HEADER_RETRY, HEADER_RETRY_VALUE)
            .build()

        return chain.proceed(retried)
    }

    companion object {
        private const val HEADER_RETRY = "X-Auth-Retry"
        private const val HEADER_RETRY_VALUE = "1"
    }
}
