package com.example.datarecorder.network

import com.example.datarecorder.model.AccountStatusRequest
import com.example.datarecorder.model.AccountStatusResponse
import com.example.datarecorder.model.ApiResponse
import com.example.datarecorder.model.AppVersionInfo
import com.example.datarecorder.model.LoginRequest
import com.example.datarecorder.model.LoginResponse
import com.example.datarecorder.model.RegisterRequest
import com.example.datarecorder.model.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginResponse>

    @POST("/api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): ApiResponse<RegisterResponse>

    @POST("/api/auth/check-status")
    suspend fun checkAccountStatus(
        @Body request: AccountStatusRequest
    ): ApiResponse<AccountStatusResponse>

    @GET("/api/app/version/latest")
    suspend fun getLatestVersion(): ApiResponse<AppVersionInfo>
}
