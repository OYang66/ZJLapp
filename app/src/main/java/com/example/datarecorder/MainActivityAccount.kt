package com.example.datarecorder

import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.model.AccountStatusRequest
import com.example.datarecorder.network.RetrofitClient
import kotlinx.coroutines.launch

fun MainActivity.registerLogoutReceiver() {
	if (logoutReceiverRegistered) return

	val filter = IntentFilter(ForceLogoutHelper.ACTION_FORCE_LOGOUT)

	ContextCompat.registerReceiver(
		this, forceLogoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
	)

	logoutReceiverRegistered = true
}

fun MainActivity.unregisterLogoutReceiverSafe() {
	if (!logoutReceiverRegistered) return
	try {
		unregisterReceiver(forceLogoutReceiver)
	} catch (_: Exception) {
	} finally {
		logoutReceiverRegistered = false
	}
}

fun MainActivity.goLogin(message: String) {
	SessionManager.saveLogoutReason(this, message)
	SessionManager.clearLogin(this)
	AccountStatusScheduler.stop(this)

	val intent = Intent(this, LoginActivity::class.java).apply {
		flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
	}
	startActivity(intent)
	finish()
}

fun MainActivity.checkAccountStatusNow() {
	val username = SessionManager.getUsername(this)
	if (isFinishing || isDestroyed) return
	if (username.isBlank()) {
		goLogin("登录状态已失效")
		return
	}

	lifecycleScope.launch {
		try {
			val response = RetrofitClient.api.checkAccountStatus(
				AccountStatusRequest(username = username)
			)

			val data = response.data
			if (response.code == 200 && data != null) {
				if (!data.valid) {
					ForceLogoutHelper.logout(
						this@checkAccountStatusNow,
						data.message.ifBlank { "账号状态异常，已退出登录" })
				}
			}
		} catch (_: Exception) {
		}
	}
}
