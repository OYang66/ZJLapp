package com.example.datarecorder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.datarecorder.model.AccountStatusRequest
import com.example.datarecorder.network.RetrofitClient

class AccountStatusWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val username = SessionManager.getUsername(applicationContext)
        if (username.isBlank()) {
            return Result.success()
        }

        return try {
            val response = RetrofitClient.api.checkAccountStatus(
                AccountStatusRequest(username = username)
            )

            val data = response.data
            if (response.code == 200 && data != null) {
                if (!data.valid) {
                    ForceLogoutHelper.logout(
                        applicationContext,
                        data.message.ifBlank { "账号状态异常，已退出登录" }
                    )
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
