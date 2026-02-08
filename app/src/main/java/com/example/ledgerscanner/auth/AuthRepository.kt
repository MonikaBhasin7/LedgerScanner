package com.example.ledgerscanner.auth

import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.network.AuthApi
import com.example.ledgerscanner.network.model.ApiErrorResponse
import com.example.ledgerscanner.network.model.AuthResponse
import com.example.ledgerscanner.network.model.OtpRequest
import com.example.ledgerscanner.network.model.OtpVerifyRequest
import com.example.ledgerscanner.network.model.RefreshRequest
import com.google.gson.Gson
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val gson: Gson
) {
    suspend fun requestOtp(phoneNumber: String): OperationResult<Unit> {
        return try {
            authApi.requestOtp(OtpRequest(phoneNumber))
            OperationResult.Success(Unit)
        } catch (t: Throwable) {
            OperationResult.Error(message = parseErrorMessage(t) ?: "OTP request failed", throwable = t)
        }
    }

    suspend fun verifyOtp(phoneNumber: String, otp: String): OperationResult<AuthResponse> {
        return try {
            val response = authApi.verifyOtp(OtpVerifyRequest(phoneNumber, otp))
            tokenStore.saveTokens(response.accessToken, response.refreshToken)
            OperationResult.Success(response)
        } catch (t: Throwable) {
            OperationResult.Error(message = parseErrorMessage(t) ?: "OTP verify failed", throwable = t)
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
            OperationResult.Error(message = parseErrorMessage(t) ?: "Token refresh failed", throwable = t)
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
            OperationResult.Error(message = parseErrorMessage(t) ?: "Logout failed", throwable = t)
        }
    }

    fun isLoggedIn(): Boolean = !tokenStore.getAccessToken().isNullOrBlank()

    private fun parseErrorMessage(t: Throwable): String? {
        if (t is HttpException) {
            val errorBody = t.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                return try {
                    val parsed = gson.fromJson(errorBody, ApiErrorResponse::class.java)
                    parsed.message?.takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                    null
                }
            }
        }
        return t.message
    }
}
