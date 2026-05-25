package com.example.adrive.ui.drive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adrive.data.model.QuotaInfo
import com.example.adrive.ui.theme.AdriveBlue

// ── App drawer ────────────────────────────────────────────────────────────────

@Composable
fun AppDrawer(
    quota: QuotaInfo,
    currentView: NavView,
    onNavigate: (NavView) -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp)) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = AdriveBlue, shape = CircleShape) {
                    Icon(
                        Icons.Default.HardDrive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Adrive", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Personal cloud storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        // Navigation items
        DrawerItem(
            icon = Icons.Default.CloudQueue,
            label = "My Drive",
            selected = currentView == NavView.DRIVE,
            onClick = { onNavigate(NavView.DRIVE) }
        )
        DrawerItem(
            icon = Icons.Default.Delete,
            label = "Trash",
            selected = currentView == NavView.TRASH,
            onClick = { onNavigate(NavView.TRASH) }
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        // Quota display
        QuotaDisplay(quota = quota)

        Spacer(Modifier.height(8.dp))

        DrawerItem(
            icon = Icons.Default.Logout,
            label = "Sign out",
            selected = false,
            onClick = onSignOut
        )
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun QuotaDisplay(quota: QuotaInfo) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatBytes(quota.totalBytes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "${quota.fileCount} files",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (quota.trashCount > 0) {
            Text(
                "Trash: ${quota.trashCount} files (${formatBytes(quota.trashBytes)})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

