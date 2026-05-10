package com.example.datarecorder

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "user_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_LOGOUT_REASON = "logout_reason"
    private const val KEY_ONLINE_STATUS = "online_status"
    private const val KEY_LAST_ACTIVE_TIME = "last_active_time"

    private const val KEY_REMEMBER = "remember_account_password"
    private const val KEY_SAVED_USERNAME = "saved_username"
    private const val KEY_SAVED_PASSWORD = "saved_password"

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

    fun saveOnlineState(context: Context, onlineStatus: String?, lastActiveTime: String?) {
        prefs(context).edit()
            .putString(KEY_ONLINE_STATUS, onlineStatus ?: "")
            .putString(KEY_LAST_ACTIVE_TIME, lastActiveTime ?: "")
            .apply()
    }

    fun getOnlineStatus(context: Context): String {
        return prefs(context).getString(KEY_ONLINE_STATUS, "") ?: ""
    }

    fun getLastActiveTime(context: Context): String {
        return prefs(context).getString(KEY_LAST_ACTIVE_TIME, "") ?: ""
    }

    fun clearLogin(context: Context) {
        prefs(context).edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USERNAME)
            .remove(KEY_USER_ID)
            .remove(KEY_ONLINE_STATUS)
            .remove(KEY_LAST_ACTIVE_TIME)
            .apply()
    }

    fun saveRememberedAccount(context: Context, username: String, password: String) {
        prefs(context).edit()
            .putBoolean(KEY_REMEMBER, true)
            .putString(KEY_SAVED_USERNAME, username)
            .putString(KEY_SAVED_PASSWORD, password)
            .apply()
    }

    fun clearRememberedAccount(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_REMEMBER, false)
            .remove(KEY_SAVED_USERNAME)
            .remove(KEY_SAVED_PASSWORD)
            .apply()
    }

    fun isRememberEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_REMEMBER, false)
    }

    fun getSavedUsername(context: Context): String {
        return prefs(context).getString(KEY_SAVED_USERNAME, "") ?: ""
    }

    fun getSavedPassword(context: Context): String {
        return prefs(context).getString(KEY_SAVED_PASSWORD, "") ?: ""
    }
}
