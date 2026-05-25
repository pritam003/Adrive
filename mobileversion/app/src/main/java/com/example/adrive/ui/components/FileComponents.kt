package com.example.adrive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.adrive.data.model.DriveFile
import com.example.adrive.data.model.DriveFolder
import com.example.adrive.data.model.UploadStatus
import com.example.adrive.ui.drive.colorForType
import com.example.adrive.ui.drive.fileTypeIcon
import com.example.adrive.ui.drive.formatBytes
import com.example.adrive.ui.theme.BrandGradient
import com.example.adrive.ui.theme.Indigo600
import com.example.adrive.ui.theme.Subtext

// ── File card (grid view) ─────────────────────────────────────────────────────

@Composable
fun FileCard(
    file: DriveFile,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    var menuVisible by remember { mutableStateOf(false) }
    val (bgTint, iconTint) = colorForType(file.contentType)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            // Thumbnail / icon area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                val isImage = file.contentType.startsWith("image/")
                if (isImage && file.thumbnailUrl != null) {
                    AsyncImage(
                        model = file.thumbnailUrl,
                        contentDescription = file.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        fileTypeIcon(file.contentType),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = iconTint
                    )
                }
            }

            // Footer: name + size + menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatBytes(file.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = Subtext
                    )
                }
                Box {
                    IconButton(onClick = { menuVisible = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(18.dp),
                            tint = Subtext
                        )
                    }
                    DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                        DropdownMenuItem(
                            text = { Text("Preview") },
                            onClick = { menuVisible = false; onClick() },
                            leadingIcon = { Icon(Icons.Default.Visibility, null, tint = Indigo600) }
                        )
                        DropdownMenuItem(
                            text = { Text("Download") },
                            onClick = { menuVisible = false; onDownloadClick() },
                            leadingIcon = { Icon(Icons.Default.Download, null, tint = Indigo600) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menuVisible = false; onRenameClick() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = { menuVisible = false; onShareClick() },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Move to trash", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuVisible = false; onDeleteClick() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    }
}

// ── Folder card (grid view) — gradient pastel ─────────────────────────────────

@Composable
fun FolderCard(folder: DriveFolder, onClick: () -> Unit) {
    val gradient = folderGradientFor(folder.name)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(gradient)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Folder, null, tint = Color(0xFFB45309))
            }
            Text(
                folder.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun folderGradientFor(name: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFFFEF3C7), Color(0xFFFED7AA)),    // amber → orange
        listOf(Color(0xFFE0E7FF), Color(0xFFDDD6FE)),    // indigo → violet
        listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0)),    // emerald
        listOf(Color(0xFFFCE7F3), Color(0xFFFBCFE8)),    // pink
        listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD)),    // sky
        listOf(Color(0xFFFEE2E2), Color(0xFFFECACA)),    // red soft
    )
    val idx = (name.hashCode() and 0x7FFFFFFF) % palettes.size
    return Brush.linearGradient(palettes[idx])
}

// ── Upload progress bar ───────────────────────────────────────────────────────

@Composable
fun UploadProgressBar(item: com.example.adrive.data.model.UploadItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                val statusIcon = when (item.status) {
                    UploadStatus.DONE -> Icons.Default.CheckCircle
                    UploadStatus.ERROR -> Icons.Default.Error
                    else -> Icons.Default.CloudUpload
                }
                val statusColor = when (item.status) {
                    UploadStatus.DONE -> Color(0xFF22C55E)
                    UploadStatus.ERROR -> MaterialTheme.colorScheme.error
                    else -> Indigo600
                }
                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "${(item.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Subtext,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(item.progress)
                    .clip(CircleShape)
                    .background(BrandGradient)
            )
        }
    }
}

