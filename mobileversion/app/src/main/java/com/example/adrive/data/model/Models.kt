package com.example.adrive.data.model

import com.google.gson.annotations.SerializedName

// ── Drive items ──────────────────────────────────────────────────────────────

data class DriveFile(
    val name: String,
    val displayName: String,
    val size: Long,
    val contentType: String,
    val lastModified: String?,
    val metadata: Map<String, String> = emptyMap(),
    val readSasUrl: String? = null,
    val thumbnailUrl: String? = null
)

data class DriveFolder(
    val name: String,
    val displayName: String
)

data class ListResponse(
    val folders: List<DriveFolder>,
    val files: List<DriveFile>
)

data class QuotaInfo(
    val totalBytes: Long,
    val fileCount: Int,
    val trashBytes: Long = 0,
    val trashCount: Int = 0
)

// ── Trash ────────────────────────────────────────────────────────────────────

data class TrashItem(
    val trashKey: String,
    val originalPath: String,
    val size: Long,
    val contentType: String,
    val deletedAt: Long?,
    val readSasUrl: String
)

data class TrashListResponse(val items: List<TrashItem>)

// ── Auth ─────────────────────────────────────────────────────────────────────

data class MeResponse(
    val authenticated: Boolean,
    val userId: String? = null,
    val userDetails: String? = null,
    val identityProvider: String? = null,
    val isOwner: Boolean? = null,
    val ownerConfigured: Boolean = false
)

data class DeviceCodeInfo(
    @SerializedName("device_code") val deviceCode: String,
    @SerializedName("user_code") val userCode: String,
    @SerializedName("verification_uri") val verificationUri: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val interval: Int,
    val message: String? = null
)

data class DeviceCodeStatusResponse(
    val status: String,         // "pending" | "success" | "expired" | "error"
    val userId: String? = null,
    val name: String? = null,
    val error: String? = null
)

// ── Share ────────────────────────────────────────────────────────────────────

data class ShareCreateResponse(val token: String, val name: String)

data class ShareInfo(
    val token: String,
    val displayName: String,
    val size: Long,
    val contentType: String,
    val sasUrl: String,
    val thumbnailUrl: String? = null
)

// ── SAS / misc ───────────────────────────────────────────────────────────────

data class SasResponse(val url: String)
data class FolderRequest(val path: String)
data class RenameRequest(val from: String, val to: String)
data class ShareRequest(val name: String)
data class RestoreRequest(val trashKey: String)
data class DeviceCodePollRequest(@SerializedName("device_code") val deviceCode: String)

// ── Upload progress (in-memory only) ─────────────────────────────────────────

data class UploadItem(
    val id: String,
    val name: String,
    val progress: Float,          // 0..1
    val status: UploadStatus
)

enum class UploadStatus { QUEUED, UPLOADING, DONE, ERROR }

