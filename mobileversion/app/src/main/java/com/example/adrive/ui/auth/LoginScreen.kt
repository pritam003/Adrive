package com.example.adrive.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.example.adrive.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

private val TAGLINES = listOf(
    "Your stuff, anywhere. ✨",
    "Drop, share, smile. 📁",
    "Like Google Drive, but yours. 🚀",
    "Unlimited everything. ∞",
    "Move fast. Lose nothing. 🎯",
    "Your cloud. Your rules. 🌩️",
)

private val FLOATING_EMOJIS = listOf("📁", "📄", "🖼️", "🎵", "🎬", "📊", "✨", "☁️", "🚀", "💾", "🎨", "📸")

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
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated gradient background
            AnimatedGradientBackground()
            // Floating emoji confetti
            FloatingEmojis()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LogoSection()

                Spacer(Modifier.height(28.dp))

                // Glass card holds the actual state UI
                GlassCard {
                    AnimatedContent(
                        targetState = state::class,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(200)))
                        },
                        label = "auth"
                    ) {
                        when (val s = state) {
                            is AuthState.Checking ->
                                CenterLoading()

                            is AuthState.Unauthenticated ->
                                UnauthSection(onSignIn = { vm.startLogin() })

                            is AuthState.DeviceCodeFlow ->
                                DeviceCodeSection(
                                    info = s,
                                    context = context,
                                    onCancel = { vm.cancelLogin() }
                                )

                            is AuthState.AccessDenied ->
                                AccessDeniedSection(
                                    userDetails = s.userDetails,
                                    onSignOut = { vm.signOut() }
                                )

                            is AuthState.Error ->
                                ErrorSection(
                                    message = s.message,
                                    onRetry = { vm.startLogin() }
                                )

                            is AuthState.Authenticated ->
                                CenterLoading(text = "Signing you in…")
                        }
                    }
                }
            }
        }
    }
}

// ─── Animated gradient background ───────────────────────────────────────────

@Composable
private fun AnimatedGradientBackground() {
    val infinite = rememberInfiniteTransition(label = "bg")
    val shift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift"
    )

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val brush = if (dark) LoginBgGradientDark else LoginBgGradient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    ) {
        // Two soft animated "blobs"
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(
                    x = (40 + 30 * sin(shift * 2 * Math.PI).toFloat()).dp,
                    y = (60 + 40 * cos(shift * 2 * Math.PI).toFloat()).dp,
                )
                .clip(CircleShape)
                .background(Color(0x33A78BFA))
                .blur(60.dp)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(
                    x = (-30 - 30 * cos(shift * 2 * Math.PI).toFloat()).dp,
                    y = (-100 - 40 * sin(shift * 2 * Math.PI).toFloat()).dp,
                )
                .clip(CircleShape)
                .background(Color(0x336366F1))
                .blur(60.dp)
        )
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

// ─── Floating emojis ────────────────────────────────────────────────────────

@Composable
private fun FloatingEmojis() {
    val infinite = rememberInfiniteTransition(label = "emojis")
    Box(modifier = Modifier.fillMaxSize()) {
        FLOATING_EMOJIS.forEachIndexed { i, e ->
            val delay = (i * 700) % 8000
            val duration = 14000 + (i * 900) % 6000
            val rise by infinite.animateFloat(
                initialValue = 1f, targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    tween(duration, delayMillis = delay, easing = LinearEasing),
                    RepeatMode.Restart
                ),
                label = "rise$i"
            )
            val leftPct = (5 + (i * 37) % 90)
            val sizeSp = (20 + (i * 5) % 18)
            Text(
                text = e,
                fontSize = sizeSp.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(unbounded = true)
                    .offsetPercent(leftPct, rise)
            )
        }
    }
}

@Composable
private fun Modifier.offsetPercent(leftPct: Int, risePct: Float): Modifier {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val x = (config.screenWidthDp * leftPct / 100f).dp
    val y = (config.screenHeightDp * risePct).dp
    return this.then(Modifier.offset(x, y))
}

// ─── Logo (pulsing, glowing) ────────────────────────────────────────────────

@Composable
private fun LogoSection() {
    val infinite = rememberInfiniteTransition(label = "logo")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    // rotating gradient ring
    val rot by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )

    var taglineIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3500)
            taglineIdx = (taglineIdx + 1) % TAGLINES.size
        }
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(pulse),
        contentAlignment = Alignment.Center
    ) {
        // outer glow
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0x66A78BFA), Color.Transparent)))
                .blur(20.dp)
        )
        // gradient ring
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(BrandGradient)
        )
        // inner white circle holding the icon
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.CloudQueue,
                contentDescription = "Adrive",
                modifier = Modifier.size(44.dp),
                tint = Indigo600
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(
        "Adrive",
        fontSize = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(4.dp))
    AnimatedContent(
        targetState = taglineIdx,
        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
        label = "tagline"
    ) { idx ->
        Text(
            TAGLINES[idx],
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Glass card wrapper ─────────────────────────────────────────────────────

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.padding(24.dp)) { content() }
    }
}

@Composable
private fun CenterLoading(text: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Indigo600,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
        if (text != null) {
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Subtext)
        }
    }
}

// ─── Unauth (sign in CTA + perks) ───────────────────────────────────────────

@Composable
private fun UnauthSection(onSignIn: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ShimmerButton(onClick = onSignIn)
        Spacer(Modifier.height(20.dp))
        Text(
            "We use Microsoft device code login.\nNo passwords. No tracking. Just yours.",
            style = MaterialTheme.typography.bodyMedium,
            color = Subtext,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PerkChip("∞", "Unlimited")
            PerkChip("🔒", "Private")
            PerkChip("⚡", "Fast")
        }
    }
}

@Composable
private fun RowScope.PerkChip(emoji: String, label: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
private fun ShimmerButton(onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val shimmer by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "x"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(BrandGradient)
    ) {
        // shimmer overlay
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(140.dp)
                .offset(x = ((shimmer * 600) - 140).dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Login, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                "Continue with Microsoft",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ─── Device code section ────────────────────────────────────────────────────

@Composable
private fun DeviceCodeSection(
    info: AuthState.DeviceCodeFlow,
    context: Context,
    onCancel: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1800); copied = false }
    }

    fun copyCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("code", info.info.userCode))
        copied = true
    }

    fun openBrowser() {
        runCatching {
            CustomTabsIntent.Builder().setShowTitle(true).build()
                .launchUrl(context, Uri.parse(info.info.verificationUri))
        }.onFailure {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.info.verificationUri)))
        }
    }

    LaunchedEffect(Unit) { openBrowser() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepLabel(num = 1, text = "Copy this code")
        Spacer(Modifier.height(10.dp))

        // Big tappable code card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEEF2FF),
            border = androidx.compose.foundation.BorderStroke(2.dp, Indigo600.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    info.info.userCode,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = Indigo600,
                    letterSpacing = 6.sp
                )
                FilledIconButton(
                    onClick = ::copyCode,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Indigo600)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color.White)
                }
            }
        }

        AnimatedContent(targetState = copied, label = "copied") { isCopied ->
            if (isCopied) {
                Text(
                    "✓ Copied to clipboard",
                    color = Success,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        StepLabel(num = 2, text = "Sign in on Microsoft")
        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = ::openBrowser,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Indigo600)
        ) {
            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = Indigo600)
            Spacer(Modifier.width(8.dp))
            Text("Open Microsoft sign-in", color = Indigo600, fontWeight = FontWeight.SemiBold)
        }

        if (info.polling) {
            Spacer(Modifier.height(20.dp))
            PulsingWaitingDots()
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onCancel) {
            Text("← Start over", color = Subtext)
        }
    }
}

@Composable
private fun StepLabel(num: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(BrandGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                num.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PulsingWaitingDots() {
    val infinite = rememberInfiniteTransition(label = "dots")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val alpha by infinite.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(800, delayMillis = i * 200, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = alpha))
            )
            if (i < 2) Spacer(Modifier.width(6.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Waiting for you to sign in…",
            style = MaterialTheme.typography.bodyMedium,
            color = Subtext
        )
    }
}

// ─── Access denied / Error ──────────────────────────────────────────────────

@Composable
private fun AccessDeniedSection(userDetails: String?, onSignOut: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🚫", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Access denied",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "This Adrive is private.\nSigned in as ${userDetails ?: "someone else"}.",
            style = MaterialTheme.typography.bodyMedium,
            color = Subtext,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onSignOut,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
        ) {
            Text("Sign out", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorSection(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚠️", fontSize = 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Try again", fontWeight = FontWeight.SemiBold)
        }
    }
}

