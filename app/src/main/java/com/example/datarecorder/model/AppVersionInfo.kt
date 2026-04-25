package com.example.datarecorder.model

data class AppVersionInfo(
    val id: Long? = null,
    val versionCode: Int,
    val versionName: String,
    val updateTitle: String? = "",
    val updateContent: String? = "",
    val downloadUrl: String,
    val forceUpdate: Int = 0,
    val status: String? = "0"
)

