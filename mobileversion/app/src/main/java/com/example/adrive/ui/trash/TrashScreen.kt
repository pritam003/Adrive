package com.example.adrive.ui.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adrive.data.model.TrashItem
import com.example.adrive.ui.drive.formatBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrashScreen(vm: TrashViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmEmptyTrash by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Empty trash button (if items exist)
        if (state.items.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { confirmEmptyTrash = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Empty trash")
                }
            }
            HorizontalDivider()
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗑️", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Trash is empty", style = MaterialTheme.typography.titleMedium)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.items, key = { it.trashKey }) { item ->
                    TrashListItem(
                        item = item,
                        onRestore = { vm.restore(item.trashKey) },
                        onPurge = { vm.purge(item.trashKey) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Empty trash?") },
            text = { Text("This will permanently delete all ${state.items.size} items. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { vm.purgeAll(); confirmEmptyTrash = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { confirmEmptyTrash = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TrashListItem(item: TrashItem, onRestore: () -> Unit, onPurge: () -> Unit) {
    val name = item.originalPath.substringAfterLast('/')
    val deletedAt = item.deletedAt?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
    } ?: "Unknown date"

    ListItem(
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text("${formatBytes(item.size)} · Deleted $deletedAt", style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            Icon(Icons.Default.InsertDriveFile, contentDescription = null)
        },
        trailingContent = {
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onPurge) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

