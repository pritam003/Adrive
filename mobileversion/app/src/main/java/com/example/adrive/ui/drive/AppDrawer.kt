package com.example.adrive.ui.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.adrive.data.model.QuotaInfo
import com.example.adrive.ui.theme.BrandGradient
import com.example.adrive.ui.theme.Indigo600
import com.example.adrive.ui.theme.Subtext

@Composable
fun AppDrawer(
    quota: QuotaInfo,
    currentView: NavView,
    onNavigate: (NavView) -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ─── Hero header with gradient ───────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandGradient)
                .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Adrive",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Your personal cloud",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                // Quota card
                QuotaCard(quota = quota)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ─── Navigation items ────────────────────────────────────────────
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
            badge = quota.trashCount.takeIf { it > 0 }?.toString(),
            onClick = { onNavigate(NavView.TRASH) }
        )

        Spacer(Modifier.weight(1f))

        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        // ─── Sign out ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSignOut)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Sign out",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Made with ❤️ • v1.0",
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = Subtext
        )
    }
}

@Composable
private fun QuotaCard(quota: QuotaInfo) {
    val activeBytes = quota.totalBytes
    val sizeText = formatBytes(activeBytes)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.22f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Storage, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Storage used",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    sizeText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "${quota.fileCount} file${if (quota.fileCount == 1) "" else "s"}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val bg = if (selected) Indigo600.copy(alpha = 0.12f) else Color.Transparent
    val fg = if (selected) Indigo600 else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg)
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.error
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

