package com.example.ledgerscanner.network.model

import com.google.gson.annotations.SerializedName

data class OtpRequest(
    @SerializedName("phoneNumber") val phoneNumber: String
)

data class OtpVerifyRequest(
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("otp") val otp: String
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("accessTokenExpiresInMs") val accessTokenExpiresInMs: Long,
    @SerializedName("refreshTokenExpiresInMs") val refreshTokenExpiresInMs: Long,
    @SerializedName("member") val member: MemberInfo
)

data class TokenResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("accessTokenExpiresInMs") val accessTokenExpiresInMs: Long,
    @SerializedName("refreshTokenExpiresInMs") val refreshTokenExpiresInMs: Long
)

data class MemberInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("role") val role: String,
    @SerializedName("instituteId") val instituteId: Long
)
