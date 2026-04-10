package com.example.datarecorder.model

data class AccountStatusResponse(
    val valid: Boolean,
    val reason: String,
    val message: String
)
