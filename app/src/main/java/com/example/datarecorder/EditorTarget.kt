package com.example.datarecorder

data class EditorTarget(
    val mode: ModeType? = null,
    val rowIndex: Int = -1,
    val columnKey: String = ""
)
