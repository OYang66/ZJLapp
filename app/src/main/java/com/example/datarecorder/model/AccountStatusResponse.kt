package com.example.datarecorder.model

data class AccountStatusResponse(
			val valid: Boolean = true,
			val message: String = "",
			val onlineStatus: String? = null,
			val lastActiveTime: String? = null
)
