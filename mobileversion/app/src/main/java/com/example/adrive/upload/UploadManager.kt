package com.example.adrive.upload

import android.content.Context
import android.net.Uri
import com.example.adrive.data.model.UploadItem
import com.example.adrive.data.model.UploadStatus
import com.example.adrive.data.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.util.UUID

/**
 * In-process upload manager. Uploads files directly to Azure Blob Storage
 * using SAS URLs (same as the web app). Tracks progress via StateFlow.
 *
 * For uploads that must survive app restart, wire this to WorkManager.
 */
class UploadManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _uploads = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploads: StateFlow<List<UploadItem>> = _uploads.asStateFlow()

    fun enqueue(
        context: Context,
        uri: Uri,
        blobName: String,
        sasUrl: String,
        displayName: String,
        onDone: () -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        addOrUpdate(UploadItem(id, displayName, 0f, UploadStatus.QUEUED))

        scope.launch {
            try {
                addOrUpdate(UploadItem(id, displayName, 0f, UploadStatus.UPLOADING))
                uploadToSas(context, uri, sasUrl, blobName) { loaded, total ->
                    val pct = if (total > 0) loaded.toFloat() / total else 0f
                    addOrUpdate(UploadItem(id, displayName, pct.coerceIn(0f, 1f), UploadStatus.UPLOADING))
                }
                addOrUpdate(UploadItem(id, displayName, 1f, UploadStatus.DONE))
                // Remove done item after a short delay
                kotlinx.coroutines.delay(3000)
                _uploads.update { list -> list.filter { it.id != id } }
                onDone()
            } catch (e: Exception) {
                addOrUpdate(UploadItem(id, displayName, 0f, UploadStatus.ERROR))
            }
        }
    }

    private fun addOrUpdate(item: UploadItem) {
        _uploads.update { list ->
            val idx = list.indexOfFirst { it.id == item.id }
            if (idx == -1) list + item
            else list.toMutableList().also { it[idx] = item }
        }
    }

    private fun uploadToSas(
        context: Context,
        uri: Uri,
        sasUrl: String,
        blobName: String,
        onProgress: (Long, Long) -> Unit
    ) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val size = resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L

        val body = object : RequestBody() {
            override fun contentType() = mimeType.toMediaTypeOrNull()
            override fun contentLength() = size
            override fun writeTo(sink: BufferedSink) {
                resolver.openInputStream(uri)?.use { input ->
                    val source = input.source()
                    var totalWritten = 0L
                    val buffer = okio.Buffer()
                    while (true) {
                        val read = source.read(buffer, 8192)
                        if (read == -1L) break
                        sink.write(buffer, read)
                        totalWritten += read
                        onProgress(totalWritten, size)
                    }
                }
            }
        }

        val request = Request.Builder()
            .url(sasUrl)
            .put(body)
            .header("x-ms-blob-type", "BlockBlob")
            .header("x-ms-blob-content-type", mimeType)
            .build()

        val response = ApiClient.getOkHttpClient().newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Upload failed: ${response.code}")
        }
    }
}

