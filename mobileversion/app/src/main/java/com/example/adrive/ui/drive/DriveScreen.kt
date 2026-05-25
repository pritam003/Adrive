package com.example.adrive.ui.drive

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adrive.data.model.DriveFile
import com.example.adrive.data.model.DriveFolder
import com.example.adrive.data.model.UploadStatus
import com.example.adrive.ui.components.FileCard
import com.example.adrive.ui.components.FolderCard
import com.example.adrive.ui.components.UploadProgressBar
import com.example.adrive.ui.trash.TrashScreen
import com.example.adrive.ui.theme.BrandGradient
import com.example.adrive.ui.theme.Indigo600
import com.example.adrive.ui.theme.Subtext

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
    val scope = rememberCoroutineScope()
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
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                AppDrawer(
                    quota = state.quota,
                    currentView = state.navView,
                    onNavigate = {
                        vm.setNavView(it)
                        scope.launch { drawerState.close() }
                    },
                    onSignOut = onSignOut
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (state.navView == NavView.DRIVE) {
                    GradientFab(onClick = { filePicker.launch(arrayOf("*/*")) })
                }
            },
            bottomBar = {
                val active = state.uploads.filter {
                    it.status == UploadStatus.UPLOADING || it.status == UploadStatus.QUEUED
                }
                AnimatedVisibility(
                    visible = active.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    UploadBottomSheet(items = active)
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── Welcome header banner ─────────────────────────────────
                if (state.navView == NavView.DRIVE) {
                    WelcomeHeader(
                        fileCount = state.quota.fileCount,
                        onMenu = { scope.launch { drawerState.open() } },
                        viewMode = state.viewMode,
                        onToggleView = {
                            vm.setViewMode(if (state.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
                        },
                        onNewFolder = { showNewFolderDialog = true }
                    )
                    PillSearch(
                        value = state.search,
                        onChange = vm::setSearch
                    )
                    Spacer(Modifier.height(4.dp))
                } else {
                    TrashHeader(onMenu = { scope.launch { drawerState.open() } })
                }

                when (state.navView) {
                    NavView.DRIVE -> DriveContent(
                        state = state,
                        vm = vm,
                        onOpenPreview = onOpenPreview,
                        onRename = { renameTarget = it },
                        onUploadClick = { filePicker.launch(arrayOf("*/*")) }
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

// ─── Welcome header banner ──────────────────────────────────────────────────

@Composable
private fun WelcomeHeader(
    fileCount: Int,
    onMenu: () -> Unit,
    viewMode: ViewMode,
    onToggleView: () -> Unit,
    onNewFolder: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandGradient)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        greeting(),
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.9f))
                    )
                    Text(
                        "My Drive",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onNewFolder) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder", tint = Color.White)
                }
                IconButton(onClick = onToggleView) {
                    Icon(
                        if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                        contentDescription = "Toggle view",
                        tint = Color.White
                    )
                }
            }
            if (fileCount > 0) {
                Text(
                    "$fileCount file${if (fileCount == 1) "" else "s"} in your cloud",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
        }
    }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h < 5 -> "Late night 🌙"
        h < 12 -> "Good morning ☀️"
        h < 17 -> "Good afternoon 👋"
        h < 21 -> "Good evening 🌅"
        else -> "Good night ✨"
    }
}

@Composable
private fun TrashHeader(onMenu: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            Spacer(Modifier.width(4.dp))
            Text("Trash", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Pill search ────────────────────────────────────────────────────────────

@Composable
private fun PillSearch(value: String, onChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-22).dp)
            .padding(horizontal = 16.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Indigo600)
            Spacer(Modifier.width(10.dp))
            BasicSearchField(value = value, onChange = onChange)
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun RowScope.BasicSearchField(value: String, onChange: (String) -> Unit) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 6.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    "Search your drive…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Subtext
                )
            }
            inner()
        }
    )
}

// ─── Gradient FAB ───────────────────────────────────────────────────────────

@Composable
private fun GradientFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(BrandGradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Upload, contentDescription = "Upload", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Upload", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Upload bottom sheet (replaces old progress strip) ──────────────────────

@Composable
private fun UploadBottomSheet(items: List<com.example.adrive.data.model.UploadItem>) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 16.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Uploading ${items.size} file${if (items.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
            items.take(3).forEach {
                UploadProgressBar(item = it)
                Spacer(Modifier.height(8.dp))
            }
            if (items.size > 3) {
                Text(
                    "… and ${items.size - 3} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = Subtext
                )
            }
        }
    }
}

// ─── Drive content body ─────────────────────────────────────────────────────

@Composable
private fun DriveContent(
    state: DriveUiState,
    vm: DriveViewModel,
    onOpenPreview: (DriveFile) -> Unit,
    onRename: (DriveFile) -> Unit,
    onUploadClick: () -> Unit
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
            TextButton(
                onClick = { vm.navigateTo("") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(14.dp), tint = Indigo600)
                Spacer(Modifier.width(4.dp))
                Text("Home", style = MaterialTheme.typography.labelLarge, color = Indigo600)
            }
            crumbs.forEach { (name, path) ->
                Icon(Icons.Default.ChevronRight, null, tint = Subtext, modifier = Modifier.size(14.dp))
                TextButton(
                    onClick = { vm.navigateTo(path) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelLarge,
                        color = Indigo600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    val search = state.search.lowercase()
    val filteredFolders = if (search.isEmpty()) state.folders
        else state.folders.filter { it.displayName.lowercase().contains(search) }
    val filteredFiles = if (search.isEmpty()) state.files
        else state.files.filter { it.displayName.lowercase().contains(search) }

    when {
        state.loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Indigo600)
            }
        }
        filteredFolders.isEmpty() && filteredFiles.isEmpty() -> {
            EmptyState(
                isSearching = state.search.isNotEmpty(),
                onUploadClick = onUploadClick
            )
        }
        state.viewMode == ViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredFolders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel("Folders", filteredFolders.size)
                    }
                }
                items(filteredFolders, key = { "f-" + it.name }) { folder ->
                    FolderCard(folder = folder, onClick = { vm.openFolder(folder) })
                }
                if (filteredFiles.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel("Files", filteredFiles.size)
                    }
                }
                items(filteredFiles, key = { "x-" + it.name }) { file ->
                    FileCard(
                        file = file,
                        onClick = { onOpenPreview(file) },
                        onDeleteClick = { vm.deleteFile(file) },
                        onRenameClick = { onRename(file) },
                        onShareClick = { vm.createShare(file) { _ -> } },
                        onDownloadClick = {
                            file.readSasUrl?.let {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                            }
                        }
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(filteredFolders, key = { "f-" + it.name }) { folder ->
                    FolderListItem(folder = folder, onClick = { vm.openFolder(folder) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                items(filteredFiles, key = { "x-" + it.name }) { file ->
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
}

@Composable
private fun SectionLabel(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Subtext,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = Indigo600.copy(alpha = 0.12f)
        ) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Indigo600,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Empty state ────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(isSearching: Boolean, onUploadClick: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isSearching) "🔍" else "✨", fontSize = 56.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (isSearching) "No matches" else "Your drive is empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (isSearching)
                    "Try a different search term"
                else
                    "Upload your first file and it'll appear here.\nPhotos, videos, docs — anything.",
                style = MaterialTheme.typography.bodyMedium,
                color = Subtext,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!isSearching) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onUploadClick,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Upload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload your first file", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Folder list item ───────────────────────────────────────────────────────

@Composable
private fun FolderListItem(folder: DriveFolder, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(folder.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Folder, null, tint = Color(0xFFD97706))
            }
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, null, tint = Subtext)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ─── File list item ─────────────────────────────────────────────────────────

@Composable
private fun FileListItem(
    file: DriveFile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    var menuVisible by remember { mutableStateOf(false) }
    val (bgColor, iconColor) = colorForType(file.contentType)

    ListItem(
        headlineContent = {
            Text(file.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(formatBytes(file.size), style = MaterialTheme.typography.bodySmall, color = Subtext)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(fileTypeIcon(file.contentType), null, tint = iconColor)
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuVisible = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuVisible = false; onRename() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuVisible = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ─── Dialogs ────────────────────────────────────────────────────────────────

@Composable
fun NewFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreateNewFolder, null, tint = Color(0xFFD97706))
            }
        },
        title = { Text("New folder", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                shape = RoundedCornerShape(10.dp),
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RenameDialog(file: DriveFile, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(file.displayName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, tint = Indigo600)
            }
        },
        title = { Text("Rename", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("New name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && name != file.displayName) onConfirm(name.trim()) },
                shape = RoundedCornerShape(10.dp),
                enabled = name.isNotBlank() && name != file.displayName
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─── Helpers ────────────────────────────────────────────────────────────────

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
    contentType.contains("zip") || contentType.contains("compressed") -> Icons.Default.FolderZip
    else -> Icons.Default.InsertDriveFile
}

/** Returns (background, icon-tint) for a file type chip. */
fun colorForType(contentType: String): Pair<Color, Color> = when {
    contentType.startsWith("image/") -> Color(0xFFFCE7F3) to Color(0xFFDB2777)
    contentType.startsWith("video/") -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
    contentType.startsWith("audio/") -> Color(0xFFEDE9FE) to Color(0xFF7C3AED)
    contentType == "application/pdf" -> Color(0xFFFFE4E6) to Color(0xFFE11D48)
    contentType.startsWith("text/") -> Color(0xFFE0F2FE) to Color(0xFF0284C7)
    contentType.contains("zip") -> Color(0xFFFEF3C7) to Color(0xFFD97706)
    else -> Color(0xFFE0E7FF) to Indigo600
}
