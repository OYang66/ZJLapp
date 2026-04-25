package com.example.datarecorder.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val code: Int = -1,
    @SerializedName(value = "msg", alternate = ["message"])
    val message: String? = null,
    val data: T? = null
)
