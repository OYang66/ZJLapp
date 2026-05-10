package com.example.datarecorder

import android.content.ClipData
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

fun MainActivity.loadServerProjectList() {
	val activity = this

	lifecycleScope.launch {
		try {
			val response = RetrofitClient.api.getProjectList()
			if (response.code == 200 && response.data != null) {
				val list = response.data
				if (list.isEmpty()) {
					toast("服务器暂无项目")
					return@launch
				}

				val content = android.widget.LinearLayout(activity).apply {
					orientation = android.widget.LinearLayout.VERTICAL
					list.forEachIndexed { index, item ->
						addView(
							createDialogListItem(
								label = item.projectName ?: "-",
								subtitle = item.latestTime?.takeIf { it.isNotBlank() } ?: "暂无最近时间",
								accent = index == 0
							),
							android.widget.LinearLayout.LayoutParams(
								android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
								android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
							).apply {
								if (index > 0) topMargin = dp(8)
							}
						)
					}
				}

				createCardDialog(
					title = "服务器项目列表",
					subtitle = "仅展示服务器最近上传的项目记录"
				) { dlg ->
					addView(wrapDialogScroll(content))
					addView(
						createDialogActionButton("关闭", primary = false) {
							dlg.dismiss()
						},
						android.widget.LinearLayout.LayoutParams(
							android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
							dp(42)
						).apply {
							topMargin = dp(16)
						}
					)
				}.show()
			} else {
				toast(response.message ?: "获取服务器项目失败")
			}
		} catch (e: Exception) {
			e.printStackTrace()
			toast("获取服务器项目失败：${e.message ?: "未知错误"}")
		}
	}
}

fun MainActivity.loadServerStatSummary() {
	val activity = this

	lifecycleScope.launch {
		try {
			val response = RetrofitClient.api.getStatSummary()
			if (response.code == 200 && response.data != null) {
				val data = response.data
				val msg = buildString {
					appendLine("账号：${data.username ?: "-"}")
					appendLine("今日：${data.todayCount ?: 0}")
					appendLine("本周：${data.weekCount ?: 0}")
					appendLine("本月：${data.monthCount ?: 0}")
					appendLine("本季度：${data.quarterCount ?: 0}")
					appendLine("本年：${data.yearCount ?: 0}")
				}

				showInfoCardDialog(
					title = "服务器统计",
					message = msg.trim(),
					subtitle = "当前账号的服务器侧汇总数据"
				)
			} else {
				toast(response.message ?: "获取统计失败")
			}
		} catch (e: Exception) {
			e.printStackTrace()
			toast("获取统计失败：${e.message ?: "未知错误"}")
		}
	}
}

fun MainActivity.uploadCurrentModeProjectToServer() {
	if (currentProjectId <= 0L) {
		toast("请先选择项目")
		return
	}

	val activity = this

	ioExecutor.execute {
		try {
			saveScreenDataToCurrentPackage()
			saveLoadingScreenToCurrentTrip()

			if (currentModeType == ModeType.QUALITY_FEEDBACK) {
				val fileName = buildQualityFeedbackWordFileName()
				val wordBytes = buildQualityFeedbackWordBytes()

				val shareDir = File(cacheDir, "share")
				if (!shareDir.exists()) {
					shareDir.mkdirs()
				}

				val file = File(shareDir, fileName)
				file.writeBytes(wordBytes)

				val uri = FileProvider.getUriForFile(
					this,
					"${packageName}.fileprovider",
					file
				)

				runOnUiThread {
					try {
						val shareIntent = Intent(Intent.ACTION_SEND).apply {
							type = getWordMimeType()
							putExtra(Intent.EXTRA_SUBJECT, fileName)
							putExtra(Intent.EXTRA_TITLE, fileName)
							putExtra(Intent.EXTRA_STREAM, uri)
							clipData = ClipData.newRawUri(fileName, uri)
							addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
							addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						}

						val chooserIntent = Intent.createChooser(shareIntent, "分享到")

						val resInfoList = packageManager.queryIntentActivities(
							chooserIntent,
							android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
						)

						for (resolveInfo in resInfoList) {
							val targetPackageName = resolveInfo.activityInfo.packageName
							grantUriPermission(
								targetPackageName,
								uri,
								Intent.FLAG_GRANT_READ_URI_PERMISSION
							)
						}

						startActivity(chooserIntent)
					} catch (e: Exception) {
						e.printStackTrace()
						toast("分享失败：${e.message ?: "未知错误"}")
					}
				}
				return@execute
			}


			val fileName = when (currentModeType) {
				ModeType.STANDARD ->
					buildExcelFileName("${currentProjectName}_${currentBuildingName}_型号统计")

				ModeType.FAST ->
					buildExcelFileName("${currentProjectName}_${currentBuildingName}_返厂统计")

				ModeType.RETURN_LOADING ->
					buildExcelFileName("${currentProjectName}_${currentBuildingName}_返厂装车")

				ModeType.QUALITY_FEEDBACK ->
					""
			}

			val excelBytes = when (currentModeType) {
				ModeType.STANDARD ->
					buildStandardExcelBytes(currentProjectName)

				ModeType.FAST ->
					buildFastExcelBytes(currentProjectName)

				ModeType.RETURN_LOADING ->
					buildLoadingExcelBytes(currentProjectName)

				ModeType.QUALITY_FEEDBACK ->
					ByteArray(0)
			}

			val uploadDir = File(cacheDir, "upload")
			if (!uploadDir.exists()) {
				uploadDir.mkdirs()
			}

			val file = File(uploadDir, fileName)
			file.writeBytes(excelBytes)

			val requestBody = file.asRequestBody(
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull()
			)
			val multipartFile = MultipartBody.Part.createFormData("file", file.name, requestBody)

			activity.lifecycleScope.launch {
				try {
					val response = when (currentModeType) {
						ModeType.STANDARD ->
							RetrofitClient.api.uploadModelStatFile(multipartFile)

						ModeType.FAST ->
							RetrofitClient.api.uploadReturnStatFile(multipartFile)

						ModeType.RETURN_LOADING ->
							RetrofitClient.api.uploadReturnLoadFile(multipartFile)

						ModeType.QUALITY_FEEDBACK ->
							return@launch
					}

					runOnUiThread {
						if (response.code == 200) {
							toast(response.message ?: "上传成功")
						} else {
							toast(response.message ?: "上传失败")
						}
					}
				} catch (e: Exception) {
					e.printStackTrace()
					runOnUiThread {
						toast("上传失败：${e.message ?: "未知错误"}")
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			runOnUiThread {
				toast("生成上传文件失败：${e.message ?: "未知错误"}")
			}
		}
	}
}

