package com.example.ledgerscanner.network

import com.example.ledgerscanner.network.model.AuthResponse
import com.example.ledgerscanner.network.model.OtpRequest
import com.example.ledgerscanner.network.model.OtpVerifyRequest
import com.example.ledgerscanner.network.model.RefreshRequest
import com.example.ledgerscanner.network.model.TokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/otp/request")
    suspend fun requestOtp(@Body request: OtpRequest)

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse
}
