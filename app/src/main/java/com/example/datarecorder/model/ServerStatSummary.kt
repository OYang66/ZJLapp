package com.example.datarecorder.model

data class ServerStatSummary(
    val todayCount: Int? = 0,
    val weekCount: Int? = 0,
    val monthCount: Int? = 0,
    val quarterCount: Int? = 0,
    val yearCount: Int? = 0,
    val userId: Long? = null,
    val username: String? = null
)
