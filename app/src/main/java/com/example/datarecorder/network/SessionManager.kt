package com.example.datarecorder

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "user_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_LOGOUT_REASON = "logout_reason"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveLogin(context: Context, token: String?, username: String?, userId: Long?) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token ?: "")
            .putString(KEY_USERNAME, username ?: "")
            .putLong(KEY_USER_ID, userId ?: 0L)
            .apply()
    }

    fun getToken(context: Context): String {
        return prefs(context).getString(KEY_TOKEN, "") ?: ""
    }

    fun getUsername(context: Context): String {
        return prefs(context).getString(KEY_USERNAME, "") ?: ""
    }

    fun getUserId(context: Context): Long {
        return prefs(context).getLong(KEY_USER_ID, 0L)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context).isNotBlank() && getUsername(context).isNotBlank()
    }

    fun saveLogoutReason(context: Context, reason: String) {
        prefs(context).edit().putString(KEY_LOGOUT_REASON, reason).apply()
    }

    fun consumeLogoutReason(context: Context): String {
        val value = prefs(context).getString(KEY_LOGOUT_REASON, "") ?: ""
        prefs(context).edit().remove(KEY_LOGOUT_REASON).apply()
        return value
    }

    fun clearLogin(context: Context) {
        prefs(context).edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USERNAME)
            .remove(KEY_USER_ID)
            .apply()
    }
}

