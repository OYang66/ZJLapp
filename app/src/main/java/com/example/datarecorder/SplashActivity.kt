package com.example.datarecorder

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.model.AccountStatusRequest
import com.example.datarecorder.network.RetrofitClient
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		RetrofitClient.init(applicationContext)

		if (!SessionManager.isLoggedIn(this)) {
			startActivity(Intent(this, LoginActivity::class.java))
			finish()
			return
		}

		checkAccountStatusAndGo()
	}

	private fun checkAccountStatusAndGo() {
		val username = SessionManager.getUsername(this)
		if (username.isBlank()) {
			forceLogoutToLogin("登录状态已失效")
			return
		}

		lifecycleScope.launch {
			try {
				val response = RetrofitClient.api.checkAccountStatus(
					AccountStatusRequest(username = username)
				)

				val data = response.data
				if (response.code == 200 && data != null) {
					SessionManager.saveOnlineState(
						this@SplashActivity,
						data.onlineStatus,
						data.lastActiveTime
					)
					if (!data.valid) {
						forceLogoutToLogin(
							data.message.ifBlank { "账号已停用或状态异常，已退出登录" }
						)
						return@launch
					}
				}

				AccountStatusScheduler.start(this@SplashActivity)
				startActivity(Intent(this@SplashActivity, MainActivity::class.java))
				finish()
			} catch (_: Exception) {
				// 网络异常时先允许进入，避免误踢
				AccountStatusScheduler.start(this@SplashActivity)
				startActivity(Intent(this@SplashActivity, MainActivity::class.java))
				finish()
			}
		}
	}

	private fun forceLogoutToLogin(message: String) {
		SessionManager.saveLogoutReason(this, message)
		SessionManager.clearLogin(this)
		AccountStatusScheduler.stop(this)

		val intent = Intent(this, LoginActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		startActivity(intent)
		finish()
	}
}
