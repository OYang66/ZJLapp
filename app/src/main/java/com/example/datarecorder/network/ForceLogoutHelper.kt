package com.example.datarecorder

import android.content.Context
import android.content.Intent

object ForceLogoutHelper {

    const val ACTION_FORCE_LOGOUT = "com.example.datarecorder.ACTION_FORCE_LOGOUT"
    const val EXTRA_MESSAGE = "message"

    fun logout(context: Context, message: String) {
        SessionManager.saveLogoutReason(context, message)
        SessionManager.clearLogin(context)

        val intent = Intent(ACTION_FORCE_LOGOUT).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_MESSAGE, message)
        }
        context.sendBroadcast(intent)
    }
}
