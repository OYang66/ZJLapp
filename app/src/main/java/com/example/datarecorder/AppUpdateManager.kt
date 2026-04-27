package com.example.datarecorder

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.example.datarecorder.model.AppVersionInfo
import com.example.datarecorder.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

class AppUpdateManager(private val activity: Activity) {

    private val client = OkHttpClient()
    private val prefs by lazy {
        activity.getSharedPreferences("app_update_cache", Activity.MODE_PRIVATE)
    }

    private val KEY_DOWNLOADED_VERSION_CODE = "downloaded_version_code"
    private val KEY_DOWNLOADED_VERSION_NAME = "downloaded_version_name"
    private val KEY_DOWNLOADED_APK_PATH = "downloaded_apk_path"

    private var pendingInstallApkFile: File? = null
    private var waitingForUnknownAppPermission = false

    private var downloadDialog: Dialog? = null
    private var downloadProgressBar: ProgressBar? = null
    private var downloadPercentView: TextView? = null
    private var downloadSizeView: TextView? = null
    private var downloadTitleView: TextView? = null
    private var currentDownloadJob: Job? = null


    fun checkUpdate(showToastWhenNoUpdate: Boolean = true) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.updateApi.getLatestVersion()
                }

                if (response.code != 200 || response.data == null) {
                    if (showToastWhenNoUpdate) {
                        Toast.makeText(activity, "获取版本信息失败", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val serverVersion = response.data
                val currentVersionCode = AppVersionUtils.getCurrentVersionCode(activity)

                if (serverVersion.versionCode > currentVersionCode) {
                    showUpdateDialog(serverVersion)
                } else {
                    val cachedVersionCode = prefs.getInt(KEY_DOWNLOADED_VERSION_CODE, -1)
                    val cachedPath = prefs.getString(KEY_DOWNLOADED_APK_PATH, null).orEmpty()
                    val cachedFile = if (cachedPath.isBlank()) null else File(cachedPath)

                    if (cachedVersionCode <= currentVersionCode || cachedFile == null || !cachedFile.exists()) {
                        clearDownloadedApkInfo()
                    }

                    if (showToastWhenNoUpdate) {
                        Toast.makeText(activity, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (showToastWhenNoUpdate) {
                    Toast.makeText(activity, "检查更新失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun onHostResume() {
        if (!waitingForUnknownAppPermission) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        if (activity.packageManager.canRequestPackageInstalls()) {
            waitingForUnknownAppPermission = false
            pendingInstallApkFile?.let { apkFile ->
                if (apkFile.exists() && apkFile.length() > 0L) {
                    installApk(apkFile)
                } else {
                    pendingInstallApkFile = null
                    clearDownloadedApkInfo()
                    Toast.makeText(activity, "安装包不存在，请重新更新", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    fun release() {
        dismissDownloadDialog()
        currentDownloadJob?.cancel()
    }

    private fun showUpdateDialog(info: AppVersionInfo) {
        val message = buildString {
            append("最新版本：${info.versionName}\n\n")
            append(
                if (info.updateContent.isNullOrBlank()) {
                    "暂无更新说明"
                } else {
                    info.updateContent
                }
            )
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(
                if (info.updateTitle.isNullOrBlank()) {
                    "发现新版本"
                } else {
                    info.updateTitle
                }
            )
            .setMessage(message)
            .setCancelable(info.forceUpdate != 1)
            .setPositiveButton("立即更新") { _, _ ->
                startUpdate(info)
            }
            .apply {
                if (info.forceUpdate != 1) {
                    setNegativeButton("暂不更新", null)
                }

            }
            .create()

        dialog.show()
    }

    private fun startUpdate(info: AppVersionInfo) {
        val cachedApk = findReusableDownloadedApk(info)
        if (cachedApk != null && cachedApk.exists() && cachedApk.length() > 0L) {
            pendingInstallApkFile = cachedApk
            Toast.makeText(activity, "安装包已下载，正在打开安装界面", Toast.LENGTH_SHORT).show()
            installApk(cachedApk)
            return
        }

        downloadAndInstall(info)
    }

    private fun findReusableDownloadedApk(info: AppVersionInfo): File? {
        val savedVersionCode = prefs.getInt(KEY_DOWNLOADED_VERSION_CODE, -1)
        val savedPath = prefs.getString(KEY_DOWNLOADED_APK_PATH, null).orEmpty()

        if (savedVersionCode == info.versionCode && savedPath.isNotBlank()) {
            val savedFile = File(savedPath)
            if (savedFile.exists() && savedFile.length() > 0L) {
                return savedFile
            }
        }

        val fileByVersion = File(getApkDownloadDir(), "update_v${info.versionCode}.apk")
        if (fileByVersion.exists() && fileByVersion.length() > 0L) {
            saveDownloadedApkInfo(info, fileByVersion)
            return fileByVersion
        }

        return null
    }

    private fun saveDownloadedApkInfo(info: AppVersionInfo, apkFile: File) {
        prefs.edit()
            .putInt(KEY_DOWNLOADED_VERSION_CODE, info.versionCode)
            .putString(KEY_DOWNLOADED_VERSION_NAME, info.versionName)
            .putString(KEY_DOWNLOADED_APK_PATH, apkFile.absolutePath)
            .apply()
    }

    private fun clearDownloadedApkInfo() {
        prefs.edit()
            .remove(KEY_DOWNLOADED_VERSION_CODE)
            .remove(KEY_DOWNLOADED_VERSION_NAME)
            .remove(KEY_DOWNLOADED_APK_PATH)
            .apply()
    }

    private fun getApkDownloadDir(): File {
        val dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: activity.filesDir

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun deleteOldCachedApks(keepFileName: String) {
        val dir = getApkDownloadDir()
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".apk") && file.name != keepFileName) {
                kotlin.runCatching { file.delete() }
            }
        }
    }

    private fun normalizeDownloadUrl(rawUrl: String): String {
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return rawUrl
        }

        val cleanPath = if (rawUrl.startsWith("/")) rawUrl else "/$rawUrl"

        // 通过 nginx 代理访问若依后端文件
        return "http://175.178.12.210:82/prod-api$cleanPath"
    }

    private fun downloadAndInstall(info: AppVersionInfo) {
        if (currentDownloadJob?.isActive == true) {
            Toast.makeText(activity, "更新包正在下载中", Toast.LENGTH_SHORT).show()
            return
        }

        currentDownloadJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                showDownloadProgressDialog()

                val realUrl = normalizeDownloadUrl(info.downloadUrl)

                val apkFile = withContext(Dispatchers.IO) {
                    downloadApk(realUrl, info)
                }

                dismissDownloadDialog()

                if (apkFile != null && apkFile.exists() && apkFile.length() > 0L) {
                    saveDownloadedApkInfo(info, apkFile)
                    pendingInstallApkFile = apkFile
                    installApk(apkFile)
                } else {
                    Toast.makeText(activity, "APK 下载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dismissDownloadDialog()
                Toast.makeText(activity, "下载失败：${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                currentDownloadJob = null
            }
        }
    }



    private fun downloadApk(url: String, info: AppVersionInfo): File? {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) return null

        val body = response.body ?: return null
        val totalBytes = body.contentLength()

        val dir = getApkDownloadDir()
        val finalFileName = "update_v${info.versionCode}.apk"
        val tempFile = File(dir, "$finalFileName.download")
        val apkFile = File(dir, finalFileName)

        if (tempFile.exists()) {
            tempFile.delete()
        }

        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8 * 1024)
                var downloadedBytes = 0L

                while (true) {
                    val len = input.read(buffer)
                    if (len == -1) break

                    output.write(buffer, 0, len)
                    downloadedBytes += len

                    updateDownloadProgress(downloadedBytes, totalBytes)
                }

                output.flush()
            }
        }

        if (apkFile.exists()) {
            apkFile.delete()
        }

        if (!tempFile.renameTo(apkFile)) {
            tempFile.copyTo(apkFile, overwrite = true)
            tempFile.delete()
        }

        deleteOldCachedApks(keepFileName = finalFileName)

        updateDownloadProgress(totalBytes.takeIf { it > 0 } ?: apkFile.length(), totalBytes)
        return apkFile
    }


    private fun installApk(apkFile: File) {
        pendingInstallApkFile = apkFile

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                waitingForUnknownAppPermission = true
                Toast.makeText(activity, "请先允许安装未知来源应用", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(intent)
                return
            }
        }

        waitingForUnknownAppPermission = false

        val apkUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toast.makeText(activity, "无法打开安装界面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDownloadProgressDialog() {
        dismissDownloadDialog()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = dpF(18f)
            }
        }

        downloadTitleView = TextView(activity).apply {
            text = "正在下载更新"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF222222.toInt())
        }

        downloadPercentView = TextView(activity).apply {
            text = "0%"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF4E3D91.toInt())
            gravity = Gravity.END
        }

        val topRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        topRow.addView(
            downloadTitleView,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        topRow.addView(
            downloadPercentView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        downloadProgressBar = ProgressBar(
            activity,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
            progressDrawable?.setTint(0xFF6C56B3.toInt())
        }

        downloadSizeView = TextView(activity).apply {
            text = "0MB / 0MB"
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(topRow)
        root.addView(
            downloadProgressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(14)
            ).apply {
                topMargin = dp(16)
            }
        )
        root.addView(
            downloadSizeView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        downloadDialog = Dialog(activity).apply {
            setCancelable(false)
            setContentView(root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            show()
            window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.86f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun dismissDownloadDialog() {
        downloadDialog?.dismiss()
        downloadDialog = null
        downloadProgressBar = null
        downloadPercentView = null
        downloadSizeView = null
        downloadTitleView = null
    }

    private fun updateDownloadProgress(downloadedBytes: Long, totalBytes: Long) {
        activity.runOnUiThread {
            val progress = if (totalBytes > 0) {
                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            } else {
                0
            }

            downloadProgressBar?.progress = progress
            downloadPercentView?.text = "$progress%"

            val downloadedText = formatFileSize(downloadedBytes)
            val totalText = if (totalBytes > 0) formatFileSize(totalBytes) else "未知大小"
            downloadSizeView?.text = "$downloadedText / $totalText"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0MB"
        val mb = bytes / 1024.0 / 1024.0
        return "${DecimalFormat("0.00").format(mb)}MB"
    }

    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }

    private fun dpF(value: Float): Float {
        return value * activity.resources.displayMetrics.density
    }
}
