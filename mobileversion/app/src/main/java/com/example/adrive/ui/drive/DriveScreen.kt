package com.example.adrive.ui.drive

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adrive.data.model.DriveFile
import com.example.adrive.data.model.DriveFolder
import com.example.adrive.data.model.UploadStatus
import com.example.adrive.ui.components.FileCard
import com.example.adrive.ui.components.FolderCard
import com.example.adrive.ui.components.UploadProgressBar
import com.example.adrive.ui.trash.TrashScreen
import com.example.adrive.ui.theme.AdriveBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    onOpenPreview: (DriveFile) -> Unit,
    onSignOut: () -> Unit,
    vm: DriveViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<DriveFile?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) vm.uploadFiles(context, uris)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                AppDrawer(
                    quota = state.quota,
                    currentView = state.navView,
                    onNavigate = { vm.setNavView(it) },
                    onSignOut = onSignOut
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (state.navView == NavView.DRIVE) {
                            OutlinedTextField(
                                value = state.search,
                                onValueChange = vm::setSearch,
                                placeholder = { Text("Search files…") },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) }
                            )
                        } else {
                            Text("Trash")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* open drawer */ }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (state.navView == NavView.DRIVE) {
                            IconButton(onClick = {
                                vm.setViewMode(if (state.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
                            }) {
                                Icon(
                                    if (state.viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                                    contentDescription = "Toggle view"
                                )
                            }
                            IconButton(onClick = { showNewFolderDialog = true }) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (state.navView == NavView.DRIVE) {
                    FloatingActionButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        containerColor = AdriveBlue
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "Upload", tint = Color.White)
                    }
                }
            },
            bottomBar = {
                if (state.uploads.any { it.status == UploadStatus.UPLOADING || it.status == UploadStatus.QUEUED }) {
                    Surface(shadowElevation = 8.dp) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            state.uploads.filter { it.status != UploadStatus.DONE }.forEach {
                                UploadProgressBar(item = it)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {

                when (state.navView) {
                    NavView.DRIVE -> DriveContent(
                        state = state,
                        vm = vm,
                        onOpenPreview = onOpenPreview,
                        onRename = { renameTarget = it }
                    )
                    NavView.TRASH -> TrashScreen()
                }
            }
        }
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { name ->
                vm.createFolder(name)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    renameTarget?.let { file ->
        RenameDialog(
            file = file,
            onConfirm = { newName ->
                vm.renameFile(file, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
}

// ── Drive content body ────────────────────────────────────────────────────────

@Composable
private fun DriveContent(
    state: DriveUiState,
    vm: DriveViewModel,
    onOpenPreview: (DriveFile) -> Unit,
    onRename: (DriveFile) -> Unit
) {
    val context = LocalContext.current

    // Breadcrumbs
    val crumbs = vm.breadcrumbs()
    if (crumbs.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { vm.navigateTo("") }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("My Drive", style = MaterialTheme.typography.bodyMedium, color = AdriveBlue)
            }
            crumbs.forEach { (name, path) ->
                Text(" / ", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { vm.navigateTo(path) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = AdriveBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        HorizontalDivider()
    }

    val search = state.search.lowercase()
    val filteredFolders = if (search.isEmpty()) state.folders else state.folders.filter { it.displayName.lowercase().contains(search) }
    val filteredFiles = if (search.isEmpty()) state.files else state.files.filter { it.displayName.lowercase().contains(search) }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (filteredFolders.isEmpty() && filteredFiles.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📂", style = MaterialTheme.typography.headlineLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit(48f, androidx.compose.ui.unit.TextUnitType.Sp)))
                Spacer(Modifier.height(8.dp))
                Text("This folder is empty", style = MaterialTheme.typography.titleMedium)
                Text("Tap + to upload files", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    if (state.viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredFolders, key = { it.name }) { folder ->
                FolderCard(folder = folder, onClick = { vm.openFolder(folder) })
            }
            items(filteredFiles, key = { it.name }) { file ->
                FileCard(
                    file = file,
                    onClick = { onOpenPreview(file) },
                    onDeleteClick = { vm.deleteFile(file) },
                    onRenameClick = { onRename(file) },
                    onShareClick = {
                        vm.createShare(file) { _ ->
                            // Share URL construction: base + /share/{token}
                        }
                    },
                    onDownloadClick = {
                        file.readSasUrl?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filteredFolders, key = { it.name }) { folder ->
                FolderListItem(folder = folder, onClick = { vm.openFolder(folder) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
            items(filteredFiles, key = { it.name }) { file ->
                FileListItem(
                    file = file,
                    onClick = { onOpenPreview(file) },
                    onDelete = { vm.deleteFile(file) },
                    onRename = { onRename(file) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

// ── Folder list item ──────────────────────────────────────────────────────────

@Composable
private fun FolderListItem(folder: DriveFolder, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(folder.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFBC02D)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ── File list item ────────────────────────────────────────────────────────────

@Composable
private fun FileListItem(
    file: DriveFile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    var menuVisible by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(file.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(formatBytes(file.size), style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(fileTypeIcon(file.contentType), contentDescription = null, tint = AdriveBlue) },
        trailingContent = {
            Box {
                IconButton(onClick = { menuVisible = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menuVisible = false; onRename() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuVisible = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun NewFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RenameDialog(file: DriveFile, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(file.displayName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("New name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank() && name != file.displayName) onConfirm(name.trim()) }) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024 * 1024 * 1024)} GB"
}

fun fileTypeIcon(contentType: String) = when {
    contentType.startsWith("image/") -> Icons.Default.Image
    contentType.startsWith("video/") -> Icons.Default.VideoFile
    contentType.startsWith("audio/") -> Icons.Default.AudioFile
    contentType == "application/pdf" -> Icons.Default.PictureAsPdf
    contentType.startsWith("text/") -> Icons.Default.TextSnippet
    else -> Icons.Default.InsertDriveFile
}

