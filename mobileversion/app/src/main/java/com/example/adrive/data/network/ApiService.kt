package com.example.adrive.data.network

import com.example.adrive.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────────

    @GET("api/me")
    suspend fun getMe(): MeResponse

    @POST("api/auth/login")
    suspend fun startDeviceLogin(): DeviceCodeInfo

    @POST("api/auth/device-code-status")
    suspend fun pollDeviceLogin(@Body body: DeviceCodePollRequest): DeviceCodeStatusResponse

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    // ── Drive ─────────────────────────────────────────────────────────────────

    @GET("api/list")
    suspend fun listItems(@Query("prefix") prefix: String = ""): ListResponse

    @GET("api/sas")
    suspend fun getUploadSas(
        @Query("name") name: String,
        @Query("mode") mode: String = "upload"
    ): SasResponse

    @GET("api/sas")
    suspend fun getReadSas(
        @Query("name") name: String,
        @Query("mode") mode: String = "read"
    ): SasResponse

    @DELETE("api/file")
    suspend fun deleteFile(
        @Query("name") name: String,
        @Query("hard") hard: Int = 0
    ): Response<Unit>

    @POST("api/rename")
    suspend fun renameFile(@Body body: RenameRequest): Response<Unit>

    @POST("api/folder")
    suspend fun createFolder(@Body body: FolderRequest): Response<Unit>

    @GET("api/quota")
    suspend fun getQuota(): QuotaInfo

    // ── Trash ─────────────────────────────────────────────────────────────────

    @GET("api/trash")
    suspend fun listTrash(): TrashListResponse

    @POST("api/trash/restore")
    suspend fun restoreFromTrash(@Body body: RestoreRequest): Response<Unit>

    @DELETE("api/trash")
    suspend fun purgeTrashItem(@Query("key") key: String): Response<Unit>

    @DELETE("api/trash")
    suspend fun purgeAllTrash(@Query("all") all: Int = 1): Response<Unit>

    // ── Share ─────────────────────────────────────────────────────────────────

    @POST("api/share")
    suspend fun createShare(@Body body: ShareRequest): ShareCreateResponse

    @DELETE("api/share")
    suspend fun revokeShare(@Query("token") token: String): Response<Unit>

    @GET("api/share/get")
    suspend fun getShareInfo(@Query("token") token: String): ShareInfo
}

