package com.group.talihayat.ui.caretaker

// ═══════════════════════════════════════════════════════════════════════════════
//  LinkElderlyScreen.kt
//  Onboarding handshake screen — Caregiver links to Elderly Dependent's device.
//
//  Public entry point : LinkElderlyScreen(onLinkSuccess: () -> Unit)
//
//  Internal states
//  ───────────────
//  LinkMode.TYPING   → manual 8-char token entry
//  LinkMode.SCANNER  → QR viewfinder overlay
//  LinkMode.SUCCESS  → multi-phase micro-interaction → onLinkSuccess()
// ═══════════════════════════════════════════════════════════════════════════════

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import com.group.talihayat.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────
//  STATE ENUM
// ─────────────────────────────────────────────────

private enum class LinkMode { TYPING, SCANNER, SUCCESS }

// ─────────────────────────────────────────────────
//  SPRING SPECS
// ─────────────────────────────────────────────────

private val CircleSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessLow
)
private val CheckSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessMedium
)
private val TextSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness    = Spring.StiffnessMediumLow
)

// ═══════════════════════════════════════════════════════════════════════════════
//  ROOT COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkElderlyScreen(onLinkSuccess: () -> Unit) {

    // ── Screen-level state ───────────────────────────────────────────────────
    var mode          by remember { mutableStateOf(LinkMode.TYPING) }
    var token         by remember { mutableStateOf("") }
    var tokenError    by remember { mutableStateOf<String?>(null) }
    var showPassword  by remember { mutableStateOf(false) }

    // ── Success animation animatables ────────────────────────────────────────
    val circleScale   = remember { Animatable(0f) }
    val checkScale    = remember { Animatable(0f) }
    val bannerAlpha   = remember { Animatable(0f) }
    val bannerOffsetY = remember { Animatable(20f) }

    // ── Phase A–D sequencer — fires when mode becomes SUCCESS ────────────────
    LaunchedEffect(mode) {
        if (mode != LinkMode.SUCCESS) return@LaunchedEffect

        // Reset all values first
        circleScale.snapTo(0f)
        checkScale.snapTo(0f)
        bannerAlpha.snapTo(0f)
        bannerOffsetY.snapTo(20f)

        // Phase A — circle morphs in (300ms card fade handled by AnimatedContent)
        delay(320)
        circleScale.animateTo(1f, CircleSpring)

        // Phase B — glow halo is handled inline via infinite transition
        // Phase C — checkmark snaps in
        delay(150)
        checkScale.animateTo(1.1f, CheckSpring)
        checkScale.animateTo(1.0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))

        // Phase D — banner expands below
        delay(180)
        bannerAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        bannerOffsetY.animateTo(0f, TextSpring)

        // Hold 1 800ms so user can enjoy the confirmation
        delay(1_800)
        onLinkSuccess()
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(if (mode == LinkMode.SCANNER) Color.Black else LBackground),
        contentAlignment = Alignment.Center
    ) {

        // Ambient decorative blobs (visible in TYPING mode only)
        if (mode == LinkMode.TYPING) {
            AmbientBackground()
        }

        // ── Master content switcher ───────────────────────────────────────────
        AnimatedContent(
            targetState  = mode,
            transitionSpec = {
                when (targetState) {
                    LinkMode.TYPING  ->
                        (slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(200)))
                    LinkMode.SCANNER ->
                        (fadeIn(tween(280))) togetherWith (fadeOut(tween(200)))
                    LinkMode.SUCCESS ->
                        (fadeIn(tween(280))) togetherWith (fadeOut(tween(300)))
                }
            },
            label = "ModeSwitch"
        ) { currentMode ->
            when (currentMode) {

                // ── MODE 1: Manual token entry ────────────────────────────────
                LinkMode.TYPING -> TypingEntryContent(
                    token        = token,
                    onTokenChange = {
                        if (it.length <= 8) {
                            token      = it.uppercase()
                            tokenError = null
                        }
                    },
                    tokenError    = tokenError,
                    showPassword  = showPassword,
                    onTogglePass  = { showPassword = !showPassword },
                    onConnect     = {
                        when {
                            token.isBlank()  -> tokenError = "Please enter the pairing token"
                            token.length < 8 -> tokenError = "Token must be exactly 8 characters"
                            else             -> mode = LinkMode.SUCCESS
                        }
                    },
                    onScanQr      = { mode = LinkMode.SCANNER }
                )

                // ── MODE 2: QR scanner viewfinder ────────────────────────────
                LinkMode.SCANNER -> QrScannerContent(
                    onClose   = { mode = LinkMode.TYPING },
                    onScanned = { mode = LinkMode.SUCCESS }
                )

                // ── STATE 3: Pairing success animation ────────────────────────
                LinkMode.SUCCESS -> PairingSuccessContent(
                    circleScale   = circleScale.value,
                    checkScale    = checkScale.value,
                    bannerAlpha   = bannerAlpha.value,
                    bannerOffsetY = bannerOffsetY.value
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  AMBIENT BACKGROUND CANVAS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AmbientBackground() {
    val drift = rememberInfiniteTransition(label = "Drift")
    val driftAngle by drift.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label         = "DriftAngle"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rad = driftAngle * (Math.PI / 180f).toFloat()
        drawCircle(
            color  = LTealSafe.copy(alpha = 0.07f),
            radius = size.width * 0.70f,
            center = Offset(
                size.width * 0.9f + cos(rad) * size.width * 0.06f,
                -size.height * 0.05f + sin(rad) * size.height * 0.04f
            )
        )
        drawCircle(
            color  = LNavy.copy(alpha = 0.04f),
            radius = size.width * 0.50f,
            center = Offset(-size.width * 0.08f, size.height * 0.92f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MODE 1 — TYPING ENTRY CONTENT
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypingEntryContent(
    token         : String,
    onTokenChange : (String) -> Unit,
    tokenError    : String?,
    showPassword  : Boolean,
    onTogglePass  : () -> Unit,
    onConnect     : () -> Unit,
    onScanQr      : () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Logo mark ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(LTealLight, CircleShape)
                .border(2.dp, LTealSafe.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Filled.Link,
                contentDescription = null,
                tint               = LTealSafe,
                modifier           = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Welcome text block ────────────────────────────────────────────────
        Text(
            text          = "Connect Your\nLoved One",
            fontSize      = 30.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = LNavy,
            textAlign     = TextAlign.Center,
            lineHeight    = 37.sp,
            letterSpacing = (-0.5).sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text       = "Enter the 8-character pairing token shown on your elderly family member's phone, or scan the QR code on their screen.",
            fontSize   = 14.sp,
            color      = LGrayMuted,
            textAlign  = TextAlign.Center,
            lineHeight = 21.sp,
            modifier   = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(36.dp))

        // ── Token card ────────────────────────────────────────────────────────
        LinkCard {
            Column(modifier = Modifier.padding(22.dp)) {

                Text(
                    text       = "Pairing Token",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (tokenError != null) LCrimsonAlert else LGrayMuted,
                    modifier   = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value         = token,
                    onValueChange = onTokenChange,
                    placeholder   = {
                        Text(
                            "e.g.  A1 B2 C3 D4",
                            fontSize = 14.sp,
                            color    = LGrayMuted.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    },
                    leadingIcon   = {
                        Icon(
                            imageVector        = Icons.Filled.VpnKey,
                            contentDescription = null,
                            tint               = if (tokenError != null) LCrimsonAlert else LTealSafe,
                            modifier           = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon  = {
                        IconButton(onClick = onScanQr) {
                            Icon(
                                imageVector        = Icons.Outlined.QrCodeScanner,
                                contentDescription = "Scan QR code",
                                tint               = LTealSafe,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                    },
                    visualTransformation = if (!showPassword)
                        PasswordVisualTransformation('•') else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done
                    ),
                    isError       = tokenError != null,
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = LTealSafe,
                        unfocusedBorderColor = LGrayLight,
                        errorBorderColor     = LCrimsonAlert,
                        cursorColor          = LTealSafe
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize      = 18.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color         = LNavy
                    )
                )

                // Token visibility toggle row
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Inline error
                    AnimatedVisibility(
                        visible = tokenError != null,
                        enter   = expandVertically(tween(200)) + fadeIn(tween(150)),
                        exit    = shrinkVertically(tween(180)) + fadeOut(tween(120))
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                null,
                                tint     = LCrimsonAlert,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text       = tokenError.orEmpty(),
                                fontSize   = 11.sp,
                                color      = LCrimsonAlert,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Character counter
                    Text(
                        text     = "${token.length} / 8",
                        fontSize = 11.sp,
                        color    = if (token.length == 8) LTealSafe else LGrayMuted
                    )
                }

                // Show/hide token toggle
                TextButton(
                    onClick  = onTogglePass,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint     = LGrayMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text     = if (showPassword) "Hide token" else "Show token",
                        fontSize = 12.sp,
                        color    = LGrayMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── How to find token helper ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LNavyLight, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.Info,
                null,
                tint     = LNavy,
                modifier = Modifier.size(16.dp).padding(top = 1.dp)
            )
            Text(
                text       = "Ask your elderly family member to open TaliHayat on their phone and tap \"Show My Code\". " +
                        "The 8-character token will appear on their screen.",
                fontSize   = 12.sp,
                color      = LNavy,
                lineHeight = 18.sp,
                modifier   = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Primary CTA: Establish Connection ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(LTealSafe, LTealDark)),
                    shape = RoundedCornerShape(17.dp)
                )
                .shadow(
                    elevation    = 8.dp,
                    shape        = RoundedCornerShape(17.dp),
                    ambientColor = LTealSafe.copy(alpha = 0.25f),
                    spotColor    = LTealSafe.copy(alpha = 0.25f)
                )
                .clickable { onConnect() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Link, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(
                    text          = "Establish Connection",
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color.White,
                    letterSpacing = 0.3.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Secondary CTA: Scan QR ────────────────────────────────────────────
        OutlinedButton(
            onClick  = onScanQr,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape    = RoundedCornerShape(17.dp),
            border   = BorderStroke(1.5.dp, LTealSafe.copy(alpha = 0.55f)),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = LTealSafe)
        ) {
            Icon(Icons.Outlined.QrCodeScanner, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text       = "Scan QR Code Instead",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MODE 2 — QR SCANNER VIEWFINDER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QrScannerContent(
    onClose   : () -> Unit,
    onScanned : () -> Unit
) {
    // Laser bar animation
    val laser = rememberInfiniteTransition(label = "Laser")
    val laserY by laser.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )

    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {

        // Simulated dark camera background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
        )

        // Canvas: dark tint mask with transparent cutout + corners + laser
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxW  = size.width * 0.65f
            val boxH  = boxW                               // square viewfinder
            val left  = (size.width  - boxW) / 2f
            val top   = (size.height - boxH) / 2f
            val right = left + boxW
            val bot   = top  + boxH
            val rx    = with(density) { 20.dp.toPx() }   // corner radius

            // Full dark overlay
            drawRect(color = Color.Black.copy(alpha = 0.72f))

            // Punch a transparent rounded square via BlendMode.Clear
            drawRoundRect(
                color        = Color.Transparent,
                topLeft      = Offset(left, top),
                size         = Size(boxW, boxH),
                cornerRadius = CornerRadius(rx),
                blendMode    = BlendMode.Clear
            )

            // Corner alignment markers (L-shaped, white, 28dp arm)
            val arm   = with(density) { 28.dp.toPx() }
            val sw    = with(density) { 3.5.dp.toPx() }
            val white = Color.White

            // Top-left
            drawLine(white, Offset(left, top + rx), Offset(left, top + arm + rx), sw, StrokeCap.Round)
            drawLine(white, Offset(left + rx, top), Offset(left + arm + rx, top), sw, StrokeCap.Round)
            // Top-right
            drawLine(white, Offset(right, top + rx), Offset(right, top + arm + rx), sw, StrokeCap.Round)
            drawLine(white, Offset(right - rx, top), Offset(right - arm - rx, top), sw, StrokeCap.Round)
            // Bottom-left
            drawLine(white, Offset(left, bot - rx), Offset(left, bot - arm - rx), sw, StrokeCap.Round)
            drawLine(white, Offset(left + rx, bot), Offset(left + arm + rx, bot), sw, StrokeCap.Round)
            // Bottom-right
            drawLine(white, Offset(right, bot - rx), Offset(right, bot - arm - rx), sw, StrokeCap.Round)
            drawLine(white, Offset(right - rx, bot), Offset(right - arm - rx, bot), sw, StrokeCap.Round)

            // Animated laser bar — vertical position within viewfinder
            val laserActualY = top + laserY * boxH
            drawLine(
                brush       = Brush.horizontalGradient(
                    colors     = listOf(
                        LCrimsonAlert.copy(alpha = 0f),
                        LCrimsonAlert.copy(alpha = 0.9f),
                        LCrimsonAlert,
                        LCrimsonAlert.copy(alpha = 0.9f),
                        LCrimsonAlert.copy(alpha = 0f)
                    ),
                    startX     = left,
                    endX       = right
                ),
                start       = Offset(left + rx * 0.5f, laserActualY),
                end         = Offset(right - rx * 0.5f, laserActualY),
                strokeWidth = with(density) { 2.dp.toPx() }
            )
            // Laser glow smear
            drawLine(
                color       = LCrimsonAlert.copy(alpha = 0.18f),
                start       = Offset(left + rx, laserActualY),
                end         = Offset(right - rx, laserActualY),
                strokeWidth = with(density) { 8.dp.toPx() }
            )
        }

        // Top bar
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        "Close scanner",
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Scan QR Code",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(Modifier.weight(1f))
            // Torch toggle placeholder
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.FlashlightOn,
                    "Torch",
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Centre instruction text — sits above viewfinder
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .offset(y = -(LocalDensity.current.run { (170).dp })),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Point camera at QR code",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
        }

        // Bottom hint + "Enter code manually" fallback
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Align the QR code inside the frame",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(20.dp))

            // Demo: simulate a successful scan
            OutlinedButton(
                onClick  = onScanned,
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.QrCode, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simulate Successful Scan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onClose) {
                Text(
                    "Enter code manually instead",
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.70f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  STATE 3 — PAIRING SUCCESS ANIMATION
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PairingSuccessContent(
    circleScale   : Float,
    checkScale    : Float,
    bannerAlpha   : Float,
    bannerOffsetY : Float
) {
    // Phase B: continuous glow halo (infinite, starts once visible)
    val halo = rememberInfiniteTransition(label = "HaloGlow")
    val haloAlpha by halo.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.70f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloAlpha"
    )
    val haloScale by halo.animateFloat(
        initialValue  = 1.0f,
        targetValue   = 1.18f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloScale"
    )

    val density = LocalDensity.current

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(LBackground),
        contentAlignment = Alignment.Center
    ) {

        // Ambient teal wash behind everything
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color  = LTealSafe.copy(alpha = 0.06f),
                radius = size.width * 0.75f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 40.dp)
        ) {

            // ── Phase A + B: Circle with glow halo ───────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(220.dp)
            ) {
                // Outer breathing halo canvas
                Canvas(
                    modifier = Modifier
                        .size(220.dp * haloScale * circleScale)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LSuccessGreen.copy(alpha = haloAlpha * 0.4f),
                                LSuccessGreen.copy(alpha = haloAlpha * 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                }

                // Solid success ring via canvas stroke
                Canvas(
                    modifier = Modifier.size(160.dp * circleScale)
                ) {
                    val stroke = with(density) { 4.dp.toPx() }
                    drawCircle(
                        color  = LSuccessLight,
                        radius = size.minDimension / 2f
                    )
                    drawCircle(
                        color  = LSuccessGreen.copy(alpha = haloAlpha),
                        radius = size.minDimension / 2f - stroke / 2f,
                        style = Stroke(width = stroke)
                    )
                }

                // Phase C: Checkmark icon
                Icon(
                    imageVector        = Icons.Filled.CheckCircle,
                    contentDescription = "Pairing successful",
                    tint               = LSuccessGreen,
                    modifier           = Modifier
                        .size((80 * checkScale).dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                            alpha  = checkScale
                        }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Phase D: Banner text
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha        = bannerAlpha
                    translationY = with(density) { bannerOffsetY.dp.toPx() }
                }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text          = "Connection Established\nSecurely",
                        fontSize      = 24.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = LNavy,
                        textAlign     = TextAlign.Center,
                        lineHeight    = 31.sp,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text       = "Dato' Ahmad's device is now linked.\nFall monitoring will begin shortly.",
                        fontSize   = 14.sp,
                        color      = LGrayMuted,
                        textAlign  = TextAlign.Center,
                        lineHeight = 21.sp
                    )

                    Spacer(Modifier.height(22.dp))

                    // Confirmation chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        SuccessChip(label = "Encrypted")
                        SuccessChip(label = "Verified")
                        SuccessChip(label = "Monitoring On")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  SHARED: LINK CARD SHELL
// ─────────────────────────────────────────────────

@Composable
private fun LinkCard(
    modifier : Modifier = Modifier,
    content  : @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.shadow(
            elevation    = 6.dp,
            shape        = RoundedCornerShape(20.dp),
            ambientColor = LCardShadow,
            spotColor    = LCardShadow
        ),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = LSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
    }
}

// ─────────────────────────────────────────────────
//  SHARED: SUCCESS CHIP
// ─────────────────────────────────────────────────

@Composable
private fun SuccessChip(label: String) {
    Box(
        modifier = Modifier
            .background(LSuccessLight, RoundedCornerShape(20.dp))
            .border(1.dp, LSuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF2E7D32)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  USAGE
//
//  In your NavGraph or Activity:
//
//  composable("link_elderly") {
//      LinkElderlyScreen(
//          onLinkSuccess = {
//              navController.navigate("home") {
//                  popUpTo("link_elderly") { inclusive = true }
//              }
//          }
//      )
//  }
//
//  Or from SettingsScreen → ManageDependentScreen as an alternative flow.
//
//  For production, replace the token validation in onConnect with a Firebase
//  lookup, and replace the QR scanner Canvas mock with CameraX + ML Kit
//  barcode scanning.
// ═══════════════════════════════════════════════════════════════════════════════