package com.example.adrive.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HardDrive
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adrive.ui.theme.AdriveBlue
import com.example.adrive.ui.theme.AdriveTheme

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is AuthState.Authenticated) onAuthenticated()
    }

    AdriveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                LogoSection()

                Spacer(Modifier.height(48.dp))

                when (val s = state) {
                    is AuthState.Checking -> {
                        CircularProgressIndicator(color = AdriveBlue)
                    }

                    is AuthState.Unauthenticated -> {
                        SignInButton(onClick = { vm.startLogin() })
                        Spacer(Modifier.height(16.dp))
                        PerksRow()
                    }

                    is AuthState.DeviceCodeFlow -> {
                        DeviceCodeSection(
                            info = s,
                            context = context,
                            onCancel = { vm.cancelLogin() }
                        )
                    }

                    is AuthState.AccessDenied -> {
                        AccessDeniedSection(
                            userDetails = s.userDetails,
                            onSignOut = { vm.signOut() }
                        )
                    }

                    is AuthState.Error -> {
                        ErrorSection(message = s.message, onRetry = { vm.startLogin() })
                    }

                    is AuthState.Authenticated -> {
                        CircularProgressIndicator(color = AdriveBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "spin"
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(Color(0x33_1a73e8), Color(0x11_1a73e8)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.HardDrive,
            contentDescription = "Adrive",
            modifier = Modifier.size(56.dp),
            tint = AdriveBlue
        )
    }

    Spacer(Modifier.height(20.dp))

    Text(
        "Adrive",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        "Your cloud. Your rules. ☁️",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AdriveBlue)
    ) {
        Icon(Icons.Filled.Login, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Sign in with Microsoft", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PerksRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("∞ storage", "🔒 private", "⚡ fast").forEach { perk ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    perk,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DeviceCodeSection(
    info: AuthState.DeviceCodeFlow,
    context: Context,
    onCancel: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    fun copyCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("code", info.info.userCode))
        copied = true
    }

    fun openBrowser() {
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, Uri.parse(info.info.verificationUri))
        }.onFailure {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.info.verificationUri)))
        }
    }

    // Open browser automatically on first composition
    LaunchedEffect(Unit) { openBrowser() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Step 1 — Copy this code",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        // Code card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    info.info.userCode,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AdriveBlue,
                    letterSpacing = 6.sp
                )
                IconButton(onClick = ::copyCode) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code")
                }
            }
        }

        if (copied) {
            Text(
                "✓ Copied!",
                color = Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Step 2 — Sign in on Microsoft",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = ::openBrowser,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open Microsoft sign-in")
        }

        Spacer(Modifier.height(20.dp))

        if (info.polling) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Waiting for you to sign in…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onCancel) { Text("← Start over") }
    }
}

@Composable
private fun AccessDeniedSection(userDetails: String?, onSignOut: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🚫", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Access Denied", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (userDetails != null) {
            Text(
                "Signed in as $userDetails",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onSignOut, shape = RoundedCornerShape(12.dp)) { Text("Sign out") }
    }
}

@Composable
private fun ErrorSection(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚠️", fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) { Text("Try again") }
    }
}

