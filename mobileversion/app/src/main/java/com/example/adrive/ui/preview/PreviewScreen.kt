package com.example.adrive.ui.preview

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.adrive.data.model.DriveFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(file: DriveFile, onBack: () -> Unit) {
    val context = LocalContext.current
    val isImage = file.contentType.startsWith("image/")
    val isVideo = file.contentType.startsWith("video/")
    val isPdf = file.contentType == "application/pdf"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Open in external app
                    file.readSasUrl?.let { url ->
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open externally")
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when {
                isImage -> ZoomableImage(file = file)
                else -> UnsupportedPreview(file = file, onOpenExternal = {
                    file.readSasUrl?.let { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                })
            }
        }
    }
}

@Composable
private fun ZoomableImage(file: DriveFile) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val url = file.readSasUrl ?: file.thumbnailUrl ?: return

    AsyncImage(
        model = url,
        contentDescription = file.displayName,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x * scale
                    offsetY += pan.y * scale
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            ),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun UnsupportedPreview(file: DriveFile, onOpenExternal: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📄", style = MaterialTheme.typography.displayLarge, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text(
            file.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Preview not available for this file type",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenExternal) {
            Icon(Icons.Default.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open with app")
        }
    }
}

