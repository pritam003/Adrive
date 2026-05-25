package com.example.adrive.ui.drive

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adrive.data.model.*
import com.example.adrive.data.repository.DriveRepository
import com.example.adrive.upload.UploadManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DriveUiState(
    val prefix: String = "",
    val folders: List<DriveFolder> = emptyList(),
    val files: List<DriveFile> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val quota: QuotaInfo = QuotaInfo(0, 0),
    val search: String = "",
    val viewMode: ViewMode = ViewMode.GRID,
    val navView: NavView = NavView.DRIVE,
    val uploads: List<UploadItem> = emptyList()
)

enum class ViewMode { GRID, LIST }
enum class NavView { DRIVE, TRASH }

class DriveViewModel : ViewModel() {

    private val repo = DriveRepository()

    private val _state = MutableStateFlow(DriveUiState())
    val state: StateFlow<DriveUiState> = _state.asStateFlow()

    private val uploadManager = UploadManager()

    init {
        refresh()
        viewModelScope.launch {
            uploadManager.uploads.collect { uploads ->
                _state.update { it.copy(uploads = uploads) }
            }
        }
    }

    fun refresh() {
        val prefix = _state.value.prefix
        if (_state.value.navView != NavView.DRIVE) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.listItems(prefix).fold(
                onSuccess = { data ->
                    _state.update { it.copy(loading = false, folders = data.folders, files = data.files) }
                    refreshQuota()
                },
                onFailure = { _state.update { it.copy(loading = false, error = it.error ?: "Failed to load") } }
            )
        }
    }

    private fun refreshQuota() {
        viewModelScope.launch {
            repo.getQuota().onSuccess { q -> _state.update { it.copy(quota = q) } }
        }
    }

    fun setSearch(s: String) = _state.update { it.copy(search = s) }
    fun setViewMode(m: ViewMode) = _state.update { it.copy(viewMode = m) }
    fun setNavView(v: NavView) {
        _state.update { it.copy(navView = v) }
        if (v == NavView.DRIVE) refresh()
    }

    fun openFolder(folder: DriveFolder) {
        _state.update { it.copy(prefix = folder.name) }
        refresh()
    }

    fun navigateUp() {
        val prefix = _state.value.prefix
        if (prefix.isEmpty()) return
        val trimmed = prefix.trimEnd('/')
        val parent = if ('/' in trimmed) trimmed.substringBeforeLast('/') + "/" else ""
        _state.update { it.copy(prefix = parent) }
        refresh()
    }

    fun navigateTo(prefix: String) {
        _state.update { it.copy(prefix = prefix) }
        refresh()
    }

    fun deleteFile(file: DriveFile) {
        viewModelScope.launch {
            repo.deleteFile(file.name).onSuccess { refresh() }
        }
    }

    fun renameFile(file: DriveFile, newName: String) {
        val newPath = _state.value.prefix + newName
        viewModelScope.launch {
            repo.renameFile(file.name, newPath).onSuccess { refresh() }
        }
    }

    fun createFolder(name: String) {
        val path = _state.value.prefix + name
        viewModelScope.launch {
            repo.createFolder(path).onSuccess { refresh() }
        }
    }

    fun uploadFiles(context: Context, uris: List<Uri>) {
        val prefix = _state.value.prefix
        viewModelScope.launch {
            uris.forEach { uri ->
                val fileName = resolveFileName(context, uri) ?: uri.lastPathSegment ?: "file"
                val blobName = prefix + fileName
                repo.getUploadSas(blobName).onSuccess { sasUrl ->
                    uploadManager.enqueue(context, uri, blobName, sasUrl, fileName) {
                        refresh()
                    }
                }
            }
        }
    }

    fun createShare(file: DriveFile, onShare: (String) -> Unit) {
        viewModelScope.launch {
            repo.createShare(file.name).onSuccess { resp ->
                val shareTitle = resp.name.substringAfterLast('/')
                onShare(shareTitle)
            }
        }
    }

    /** Returns breadcrumb segments from current prefix. */
    fun breadcrumbs(): List<Pair<String, String>> {
        val prefix = _state.value.prefix
        if (prefix.isEmpty()) return emptyList()
        val segs = prefix.trimEnd('/').split('/')
        return segs.mapIndexed { idx, seg ->
            val path = segs.take(idx + 1).joinToString("/") + "/"
            Pair(seg, path)
        }
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (idx >= 0) cursor.getString(idx) else null
            }
        }.getOrNull()
    }
}

