package com.example.datarecorder.network

import com.example.datarecorder.model.AccountStatusRequest
import com.example.datarecorder.model.AccountStatusResponse
import com.example.datarecorder.model.ApiResponse
import com.example.datarecorder.model.AppConnectionCodeData
import com.example.datarecorder.model.AppConnectionCodeRequest
import com.example.datarecorder.model.AppPushReturnDataRequest
import com.example.datarecorder.model.AppPushReturnDataResult
import com.example.datarecorder.model.AppSubDisplayConnectionStatus
import com.example.datarecorder.model.AppVersionInfo
import com.example.datarecorder.model.LoginRequest
import com.example.datarecorder.model.LoginResponse
import com.example.datarecorder.model.RegisterRequest
import com.example.datarecorder.model.RegisterResponse
import com.example.datarecorder.model.ServerBuildingItem
import com.example.datarecorder.model.ServerProjectItem
import com.example.datarecorder.model.ServerStatSummary
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

		@POST("app/auth/login")
		suspend fun login(
				@Body request: LoginRequest
		): ApiResponse<LoginResponse>

		@POST("app/auth/register")
		suspend fun register(
				@Body request: RegisterRequest
		): ApiResponse<RegisterResponse>

		@POST("app/auth/checkStatus")
		suspend fun checkAccountStatus(
				@Body request: AccountStatusRequest
		): ApiResponse<AccountStatusResponse>

		@POST("app/auth/active")
		suspend fun reportActive(): ApiResponse<AccountStatusResponse>

		@GET("api/app/version/latest")
		suspend fun getLatestVersion(): ApiResponse<AppVersionInfo>

		@FormUrlEncoded
		@POST("app/auth/checkRegisterAccount")
		suspend fun checkRegisterAccount(
				@Field("accountName") accountName: String
		): ApiResponse<Any>

		@GET("app/project/list")
		suspend fun getProjectList(): ApiResponse<List<ServerProjectItem>>

		@GET("app/stat/summary")
		suspend fun getStatSummary(): ApiResponse<ServerStatSummary>

		@Multipart
		@POST("app/upload/modelStatFile")
		suspend fun uploadModelStatFile(
				@Part file: MultipartBody.Part
		): ApiResponse<Any>

		@Multipart
		@POST("app/upload/returnStatFile")
		suspend fun uploadReturnStatFile(
				@Part file: MultipartBody.Part
		): ApiResponse<Any>

		@Multipart
		@POST("app/upload/returnLoadFile")
		suspend fun uploadReturnLoadFile(
				@Part file: MultipartBody.Part
		): ApiResponse<Any>

		@GET("app/projectInfo/list")
		suspend fun getServerProjectInfoList(
				@retrofit2.http.Query("keyword") keyword: String? = null
		): ApiResponse<List<ServerProjectItem>>

		@GET("app/projectInfo/buildings")
		suspend fun getServerProjectBuildings(
				@retrofit2.http.Query("projectName") projectName: String
		): ApiResponse<List<ServerBuildingItem>>

		@POST("api/app/generate-code")
		suspend fun generateSubDisplayCode(): ApiResponse<AppConnectionCodeData>

		@POST("api/app/connection-status")
		suspend fun getSubDisplayConnectionStatus(
				@Body request: AppConnectionCodeRequest
		): ApiResponse<AppSubDisplayConnectionStatus>

		@POST("api/app/push-return-data")
		suspend fun pushSubDisplayReturnData(
				@Body request: AppPushReturnDataRequest
		): ApiResponse<AppPushReturnDataResult>
}
