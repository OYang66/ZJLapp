package com.example.datarecorder.network

data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val updateTitle: String = "",
    val updateContent: String = "",
    val downloadUrl: String,
    val forceUpdate: Boolean = false,
    val enabled: Boolean = true
)
