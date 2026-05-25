package com.example.adrive.data.repository

import com.example.adrive.data.model.*
import com.example.adrive.data.network.ApiClient

/**
 * Single repository that covers both drive operations and auth.
 * ViewModels use this to interact with the API.
 */
class DriveRepository {

    private val api get() = ApiClient.service

    // ── Auth ─────────────────────────────────────────────────────────────────

    suspend fun getMe(): Result<MeResponse> = runCatching { api.getMe() }

    suspend fun startDeviceLogin(): Result<DeviceCodeInfo> =
        runCatching { api.startDeviceLogin() }

    suspend fun pollDeviceLogin(deviceCode: String): Result<DeviceCodeStatusResponse> =
        runCatching { api.pollDeviceLogin(DeviceCodePollRequest(deviceCode)) }

    suspend fun logout(): Result<Unit> = runCatching {
        api.logout()
        ApiClient.getCookieJar().clearAll()
    }

    fun isLoggedIn(): Boolean = ApiClient.getCookieJar().hasAuthCookie()

    // ── Drive ─────────────────────────────────────────────────────────────────

    suspend fun listItems(prefix: String = ""): Result<ListResponse> =
        runCatching { api.listItems(prefix) }

    suspend fun getUploadSas(blobName: String): Result<String> =
        runCatching { api.getUploadSas(blobName).url }

    suspend fun getReadSas(blobName: String): Result<String> =
        runCatching { api.getReadSas(blobName, "read").url }

    suspend fun deleteFile(name: String, hard: Boolean = false): Result<Unit> =
        runCatching { api.deleteFile(name, if (hard) 1 else 0) }

    suspend fun renameFile(from: String, to: String): Result<Unit> =
        runCatching { api.renameFile(RenameRequest(from, to)) }

    suspend fun createFolder(path: String): Result<Unit> =
        runCatching { api.createFolder(FolderRequest(path)) }

    suspend fun getQuota(): Result<QuotaInfo> = runCatching { api.getQuota() }

    // ── Trash ─────────────────────────────────────────────────────────────────

    suspend fun listTrash(): Result<List<TrashItem>> =
        runCatching { api.listTrash().items }

    suspend fun restoreFromTrash(trashKey: String): Result<Unit> =
        runCatching { api.restoreFromTrash(RestoreRequest(trashKey)) }

    suspend fun purgeTrashItem(trashKey: String): Result<Unit> =
        runCatching { api.purgeTrashItem(trashKey) }

    suspend fun purgeAllTrash(): Result<Unit> =
        runCatching { api.purgeAllTrash() }

    // ── Share ─────────────────────────────────────────────────────────────────

    suspend fun createShare(blobName: String): Result<ShareCreateResponse> =
        runCatching { api.createShare(ShareRequest(blobName)) }

    suspend fun revokeShare(token: String): Result<Unit> =
        runCatching { api.revokeShare(token) }
}

