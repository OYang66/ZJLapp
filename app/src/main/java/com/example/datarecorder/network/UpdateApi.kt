package com.example.datarecorder.network

import com.example.datarecorder.model.ApiResponse
import com.example.datarecorder.model.AppVersionInfo
import retrofit2.http.GET

interface UpdateApi {

    @GET("api/app/version/latest")
    suspend fun getLatestVersion(): ApiResponse<AppVersionInfo>
}
