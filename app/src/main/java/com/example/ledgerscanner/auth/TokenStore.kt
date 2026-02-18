package com.example.ledgerscanner.auth

import android.content.Context
import com.example.ledgerscanner.network.model.MemberInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class MemberRole {
    ADMIN,
    MEMBER;

    fun isAdmin() = this == ADMIN
}

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun saveMember(member: MemberInfo) {
        prefs.edit()
            .putLong(KEY_MEMBER_ID, member.id)
            .putLong(KEY_INSTITUTE_ID, member.instituteId)
            .putString(KEY_MEMBER_NAME, member.name)
            .putString(KEY_MEMBER_PHONE, member.phoneNumber)
            .putString(KEY_MEMBER_ROLE, member.role)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getMemberId(): Long? = prefs.getLong(KEY_MEMBER_ID, -1L).takeIf { it > 0 }
    fun getInstituteId(): Long? = prefs.getLong(KEY_INSTITUTE_ID, -1L).takeIf { it > 0 }
    fun getMemberName(): String? = prefs.getString(KEY_MEMBER_NAME, null)
    fun getMemberPhone(): String? = prefs.getString(KEY_MEMBER_PHONE, null)
    fun getMemberRole(): MemberRole? {
        val role = prefs.getString(KEY_MEMBER_ROLE, null) ?: return null
        return runCatching { MemberRole.valueOf(role.uppercase()) }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_MEMBER_ID = "member_id"
        private const val KEY_INSTITUTE_ID = "institute_id"
        private const val KEY_MEMBER_NAME = "member_name"
        private const val KEY_MEMBER_PHONE = "member_phone"
        private const val KEY_MEMBER_ROLE = "member_role"
    }
}
