package com.example.ledgerscanner.network

import com.example.ledgerscanner.BuildConfig
import com.example.ledgerscanner.auth.AuthState
import com.example.ledgerscanner.auth.TokenStore
import com.example.ledgerscanner.network.model.RefreshRequest
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val gson: Gson
) : Authenticator {

    private val isRefreshing = AtomicBoolean(false)

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val refreshToken = tokenStore.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            tokenStore.clear()
            AuthState.notifyLoggedOut()
            return null
        }

        if (!isRefreshing.compareAndSet(false, true)) {
            return null
        }

        return try {
            val newTokens = refreshTokensBlocking(refreshToken) ?: run {
                tokenStore.clear()
                AuthState.notifyLoggedOut()
                return null
            }
            tokenStore.saveTokens(newTokens.first, newTokens.second)

            response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.first}")
                .build()
        } finally {
            isRefreshing.set(false)
        }
    }

    fun refreshTokensBlocking(refreshToken: String): Pair<String, String>? {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val api = retrofit.create(AuthApi::class.java)
        return runBlocking {
            try {
                val resp = api.refresh(RefreshRequest(refreshToken))
                resp.accessToken to resp.refreshToken
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }
}
