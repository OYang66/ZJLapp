package com.example.datarecorder.network

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val updateTitle: String? = null,
    val updateContent: String? = null,
    val downloadUrl: String,
    val forceUpdate: Boolean = false
)
