package com.example.datarecorder

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AccountStatusScheduler {

	private const val WORK_NAME = "account_status_check_work"

	fun start(context: Context) {
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		val request =
			PeriodicWorkRequestBuilder<AccountStatusWorker>(2, TimeUnit.HOURS)
				.setConstraints(constraints)
				.build()

		WorkManager.getInstance(context).enqueueUniquePeriodicWork(
			WORK_NAME,
			ExistingPeriodicWorkPolicy.UPDATE,
			request
		)
	}

	fun stop(context: Context) {
		WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
	}
}
