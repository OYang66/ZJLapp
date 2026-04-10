package com.example.datarecorder.model

data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val updateTitle: String?,
    val updateContent: String?,
    val downloadUrl: String,
    val forceUpdate: Boolean
)
