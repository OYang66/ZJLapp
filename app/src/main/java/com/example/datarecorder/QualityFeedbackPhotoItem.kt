package com.example.datarecorder

data class QualityFeedbackPhotoItem(
    var uriString: String = "",
    var localPath: String = "",
    var createTime: Long = System.currentTimeMillis()
)
