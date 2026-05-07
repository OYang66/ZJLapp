package com.example.datarecorder

import android.content.ContentUris
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

// =========================
// 历史数据备份
// =========================
fun MainActivity.buildHistoryBackupJson(): String {
	val json = JSONObject()
	json.put("projectId", currentProjectId)
	json.put("projectName", currentProjectName)
	json.put("savedAt", System.currentTimeMillis())
	json.put("standardContent", serializeStandardContent())
	json.put("fastContent", serializeFastContent())
	json.put("loadingContent", serializeLoadingContent())
	json.put("currentModeType", currentModeType.name)
	json.put("isFastMode", isFastMode)
	return json.toString()
}


fun MainActivity.saveHistoryBackupSnapshot() {
	if (currentProjectId <= 0L) return

	val standardContent = serializeStandardContent()
	val fastContent = serializeFastContent()
	val loadingContent = serializeLoadingContent()


	if (
		standardContent.isBlank() &&
		fastContent.isBlank() &&
		loadingContent.isBlank()

	) return

	lifecycleScope.launch(Dispatchers.IO) {
		try {
			val fileName = buildHistoryBackupFileName(currentProjectName)
			val bytes = buildHistoryBackupJson().toByteArray(Charsets.UTF_8)

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				writeHistoryBackupByMediaStore(fileName, bytes)
				trimHistoryBackupFilesMediaStore(currentProjectName, 5)
			} else {
				writeHistoryBackupLegacy(fileName, bytes)
				trimHistoryBackupFilesLegacy(currentProjectName, 5)
			}
		} catch (_: Exception) {
		}
	}
}


fun MainActivity.restoreHistoryBackup(item: BackupItem) {
	lifecycleScope.launch(Dispatchers.IO) {
		try {
			val jsonText = readHistoryBackupText(item)
			if (jsonText.isBlank()) {
				withContext(Dispatchers.Main) {
					toast("恢复失败：文件内容为空")
				}
				return@launch
			}

			val json = JSONObject(jsonText)
			val standardContent = json.optString("standardContent", "")
			val fastContent = json.optString("fastContent", "")
			val loadingContent = json.optString("loadingContent", "")
			val modeName = json.optString("currentModeType", ModeType.STANDARD.name)
			val qualityContent = json.optString("qualityContent", "")
			val backupMode = try {
				ModeType.valueOf(modeName)
			} catch (_: Exception) {
				if (json.optBoolean("isFastMode", false)) ModeType.FAST else ModeType.STANDARD
			}

			withContext(Dispatchers.Main) {
				clearStandardEditingState()
				clearFastEditingState()

				pendingReplaceStandardEditing = false
				pendingReplaceFastEditing = false
				pendingReplaceCurrentFastModel = false
				pendingReplaceCurrentStandardModel = false

				clearAllPackageMaps()
				deserializePackageStandardContent(standardContent)
				deserializePackageFastContent(fastContent)
				deserializeLoadingContent(loadingContent)


				val allNames = linkedSetOf<String>()
				allNames.addAll(packageStandardRowsMap.keys)
				allNames.addAll(packageFastRowsMap.keys)

				allNames.forEach { name ->
					packageStandardRowsMap.putIfAbsent(name, mutableListOf())
					packageCurrentStandardRowMap.putIfAbsent(name, StandardRow())
					packageFastRowsMap.putIfAbsent(name, mutableListOf())
					packageCurrentFastRowMap.putIfAbsent(name, FastRow())
				}

				if (allNames.isNotEmpty()) {
					if (currentPackageName.isBlank() || !allNames.contains(currentPackageName)) {
						currentPackageName = allNames.first()
					}
					loadPackageToScreen(currentPackageName)
				}

				if (loadingTripMap.isNotEmpty()) {
					if (currentLoadingTripName.isBlank() || !loadingTripMap.containsKey(
							currentLoadingTripName
						)
					) {
						currentLoadingTripName = loadingTripMap.keys.first()
					}
					val trip = loadingTripMap[currentLoadingTripName]
					loadingAluminumRows.clear()
					loadingAluminumRows.addAll(trip?.aluminumRows ?: emptyList())
					loadingIronRows.clear()
					loadingIronRows.addAll(trip?.ironRows ?: emptyList())
					vehicleInfo = trip?.vehicleInfo ?: VehicleInfo()
				}

				when (backupMode) {
					ModeType.STANDARD -> switchMode(ModeType.STANDARD)
					ModeType.FAST -> switchMode(ModeType.FAST)
					ModeType.RETURN_LOADING -> switchMode(ModeType.RETURN_LOADING)
					ModeType.QUALITY_FEEDBACK -> switchMode(ModeType.QUALITY_FEEDBACK)
				}


				updatePackageButtonText()
				updateDisplayTable()



				toast("历史数据已恢复")
			}
		} catch (e: Exception) {
			withContext(Dispatchers.Main) {
				toast("恢复失败：${e.message}")
			}
		}
	}
}


fun MainActivity.showHistoryBackupDialog() {
	lifecycleScope.launch(Dispatchers.IO) {
		val backups = loadRecentHistoryBackups()

		withContext(Dispatchers.Main) {
			if (backups.isEmpty()) {
				toast("暂无历史数据")
				return@withContext
			}

			val scrollView = ScrollView(this@showHistoryBackupDialog)
			val container = LinearLayout(this@showHistoryBackupDialog).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(8), dp(8), dp(8), dp(8))
			}
			scrollView.addView(container)

			val dialog = AlertDialog.Builder(this@showHistoryBackupDialog)
				.setTitle("历史数据")
				.setView(scrollView)
				.setNegativeButton("关闭", null)
				.create()

			backups.forEach { item ->
				val row = LinearLayout(this@showHistoryBackupDialog).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
					setPadding(dp(6), dp(8), dp(6), dp(8))
				}

				val fileNameView = TextView(this@showHistoryBackupDialog).apply {
					text = item.fileName
					textSize = 14f
					setTextColor(0xFF222222.toInt())
					layoutParams = LinearLayout.LayoutParams(
						0,
						LinearLayout.LayoutParams.WRAP_CONTENT,
						1f
					)
				}

				val restoreBtn = Button(this@showHistoryBackupDialog).apply {
					text = "恢复"
					textSize = 12f
					isAllCaps = false
					setOnClickListener {
						confirmRestoreHistoryBackup(item)
					}
				}

				val deleteBtn = Button(this@showHistoryBackupDialog).apply {
					text = "删除"
					textSize = 12f
					isAllCaps = false
					setOnClickListener {
						confirmDeleteHistoryBackup(item) {
							container.removeView(row)
							if (container.childCount == 0) {
								dialog.dismiss()
								toast("暂无历史数据")
							}
						}
					}
				}

				row.addView(fileNameView)
				row.addView(restoreBtn)
				row.addView(deleteBtn)
				container.addView(row)
			}

			dialog.show()
		}
	}
}


fun MainActivity.loadRecentHistoryBackups(): List<BackupItem> {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		loadRecentHistoryBackupsFromMediaStore()
	} else {
		loadRecentHistoryBackupsFromLegacy()
	}
}


fun MainActivity.loadRecentHistoryBackupsFromMediaStore(): List<BackupItem> {
	val result = mutableListOf<BackupItem>()
	val collection = MediaStore.Files.getContentUri("external")

	val projection = arrayOf(
		MediaStore.Files.FileColumns._ID,
		MediaStore.Files.FileColumns.DISPLAY_NAME,
		MediaStore.Files.FileColumns.DATE_MODIFIED
	)

	val selection =
		"${MediaStore.Files.FileColumns.RELATIVE_PATH}=? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
	val selectionArgs = arrayOf(
		historyBackupRelativePath,
		"%历史数据自动备份_%.json"
	)

	val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

	contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
		?.use { cursor ->
			val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
			val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
			val timeIndex =
				cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

			while (cursor.moveToNext()) {
				val id = cursor.getLong(idIndex)
				val name = cursor.getString(nameIndex) ?: continue
				val timeMillis = cursor.getLong(timeIndex) * 1000L
				val uri = ContentUris.withAppendedId(collection, id)

				result.add(
					BackupItem(
						id = id,
						fileName = name,
						projectName = "",
						timeMillis = timeMillis,
						uri = uri
					)
				)
			}
		}

	return result.sortedByDescending { it.timeMillis }.take(30)
}


fun MainActivity.loadRecentHistoryBackupsFromLegacy(): List<BackupItem> {
	val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
	val dir = File(root, historyBackupDirNameLegacy)
	if (!dir.exists()) return emptyList()

	val files = dir.listFiles() ?: return emptyList()

	return files
		.filter {
			it.isFile &&
					it.name.contains("历史数据自动备份_") &&
					it.name.endsWith(".json")
		}
		.sortedByDescending { it.lastModified() }
		.take(30)
		.map {
			BackupItem(
				fileName = it.name,
				projectName = "",
				timeMillis = it.lastModified(),
				file = it
			)
		}
}


fun MainActivity.deleteHistoryBackup(item: BackupItem): Boolean {
	return try {
		when {
			item.uri != null -> {
				contentResolver.delete(item.uri, null, null) > 0
			}

			item.file != null -> {
				item.file.delete()
			}

			else -> false
		}
	} catch (_: Exception) {
		false
	}
}


fun MainActivity.confirmDeleteHistoryBackup(item: BackupItem, onDeleted: () -> Unit) {
	AlertDialog.Builder(this)
		.setTitle("确认删除")
		.setMessage("是否删除该历史备份？\n删除后无法恢复。")
		.setNegativeButton("取消", null)
		.setPositiveButton("删除") { _, _ ->
			lifecycleScope.launch(Dispatchers.IO) {
				val success = deleteHistoryBackup(item)
				withContext(Dispatchers.Main) {
					if (success) {
						onDeleted()
						toast("备份已删除")
					} else {
						toast("删除失败")
					}
				}
			}
		}
		.show()
}


fun MainActivity.readHistoryBackupText(item: BackupItem): String {
	return when {
		item.uri != null -> {
			contentResolver.openInputStream(item.uri)
				?.bufferedReader(Charsets.UTF_8)
				?.use { it.readText() }
				.orEmpty()
		}

		item.file != null && item.file.exists() -> {
			item.file.readText(Charsets.UTF_8)
		}

		else -> ""
	}
}

