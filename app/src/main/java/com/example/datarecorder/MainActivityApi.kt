package com.example.datarecorder

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlinx.coroutines.launch

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

                val items = list.map { item ->
                    "${item.projectName ?: "-"}    ${item.latestTime ?: ""}"
                }.toTypedArray()

                AlertDialog.Builder(activity)
                    .setTitle("服务器项目列表")
                    .setItems(items, null)
                    .setPositiveButton("确定", null)
                    .show()
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

                AlertDialog.Builder(activity)
                    .setTitle("服务器统计")
                    .setMessage(msg)
                    .setPositiveButton("确定", null)
                    .show()
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

            val fileName = when (currentModeType) {
                ModeType.STANDARD -> buildExcelFileName("${currentProjectName}_${currentBuildingName}_型号统计")
                ModeType.FAST -> buildExcelFileName("${currentProjectName}_${currentBuildingName}_返厂统计")
                ModeType.RETURN_LOADING -> buildExcelFileName("${currentProjectName}_${currentBuildingName}_返厂装车")
            }

            val excelBytes = when (currentModeType) {
                ModeType.STANDARD -> buildStandardExcelBytes(currentProjectName)
                ModeType.FAST -> buildFastExcelBytes(currentProjectName)
                ModeType.RETURN_LOADING -> buildLoadingExcelBytes(currentProjectName)
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
                        ModeType.STANDARD -> RetrofitClient.api.uploadModelStatFile(multipartFile)
                        ModeType.FAST -> RetrofitClient.api.uploadReturnStatFile(multipartFile)
                        ModeType.RETURN_LOADING -> RetrofitClient.api.uploadReturnLoadFile(multipartFile)
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
