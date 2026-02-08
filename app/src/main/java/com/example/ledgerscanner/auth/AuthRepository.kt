package com.example.ledgerscanner.auth

import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.network.AuthApi
import com.example.ledgerscanner.network.model.AuthResponse
import com.example.ledgerscanner.network.model.OtpRequest
import com.example.ledgerscanner.network.model.OtpVerifyRequest
import com.example.ledgerscanner.network.model.RefreshRequest
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore
) {
    suspend fun requestOtp(phoneNumber: String): OperationResult<Unit> {
        return try {
            authApi.requestOtp(OtpRequest(phoneNumber))
            OperationResult.Success(Unit)
        } catch (t: Throwable) {
            OperationResult.Error(message = t.message ?: "OTP request failed", throwable = t)
        }
    }

    suspend fun verifyOtp(phoneNumber: String, otp: String): OperationResult<AuthResponse> {
        return try {
            val response = authApi.verifyOtp(OtpVerifyRequest(phoneNumber, otp))
            tokenStore.saveTokens(response.accessToken, response.refreshToken)
            OperationResult.Success(response)
        } catch (t: Throwable) {
            OperationResult.Error(message = t.message ?: "OTP verify failed", throwable = t)
        }
    }

    suspend fun refreshToken(): OperationResult<Unit> {
        val refreshToken = tokenStore.getRefreshToken()
            ?: return OperationResult.Error("Refresh token missing")
        return try {
            val response = authApi.refresh(RefreshRequest(refreshToken))
            tokenStore.saveTokens(response.accessToken, response.refreshToken)
            OperationResult.Success(Unit)
        } catch (t: Throwable) {
            OperationResult.Error(message = t.message ?: "Token refresh failed", throwable = t)
        }
    }

    suspend fun logout(): OperationResult<Unit> {
        val refreshToken = tokenStore.getRefreshToken()
        return try {
            if (!refreshToken.isNullOrBlank()) {
                authApi.logout(RefreshRequest(refreshToken))
            }
            tokenStore.clear()
            OperationResult.Success(Unit)
        } catch (t: Throwable) {
            tokenStore.clear()
            OperationResult.Error(message = t.message ?: "Logout failed", throwable = t)
        }
    }

    fun isLoggedIn(): Boolean = !tokenStore.getAccessToken().isNullOrBlank()
}
