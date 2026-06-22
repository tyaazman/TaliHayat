package com.group.talihayat.ui.elderly

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object ElderlyColors {
    val SafeBackground   = Color(0xFFF5F7FA)
    val Surface          = Color(0xFFFFFFFF)
    val PrimaryText      = Color(0xFF0D1B2A)
    val SecondaryText    = Color(0xFF3A4A5C)
    val NavyBlue         = Color(0xFF1E3A5F)
    val SafeGreen        = Color(0xFF1E8A4A)
    val SafeGreenLight   = Color(0xFFE6F4EC)
    val GrayBorder       = Color(0xFFCDD5DF)
    val NavyLight        = Color(0xFFE8EFF8)
    val TokenBg          = Color(0xFFF0F4FA)
}

private val CircleSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
private val CheckSpring  = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
private val TextSpring   = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

private enum class PairingState { DISPLAY_CODE, CONNECTING, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    pairingToken:     String      = "A1B2C3",
    countdown:        Int         = 60,
    isLinked:         Boolean     = false,
    onBackClick:      () -> Unit  = {},
    onPairingSuccess: () -> Unit  = {},
) {
    var pairingState by remember { mutableStateOf(PairingState.DISPLAY_CODE) }
    val coroutineScope = rememberCoroutineScope()

    val circleScale    = remember { Animatable(0f) }
    val checkProgress  = remember { Animatable(0f) }
    val textAlpha      = remember { Animatable(0f) }
    val buttonAlpha    = remember { Animatable(0f) }

    LaunchedEffect(isLinked) {
        if (isLinked) {
            pairingState = PairingState.CONNECTING
            delay(1500L)
            pairingState = PairingState.SUCCESS
        }
    }

    LaunchedEffect(pairingState) {
        if (pairingState != PairingState.SUCCESS) return@LaunchedEffect

        circleScale.animateTo(targetValue = 1f, animationSpec = CircleSpring)
        delay(200L)
        checkProgress.animateTo(targetValue = 1f, animationSpec = CheckSpring)
        delay(280L)
        textAlpha.animateTo(targetValue = 1f, animationSpec = TextSpring)
        delay(220L)
        buttonAlpha.animateTo(targetValue = 1f, animationSpec = TextSpring)
    }

    Scaffold(
        containerColor = ElderlyColors.SafeBackground,
        topBar = {
            AnimatedVisibility(
                visible = pairingState == PairingState.DISPLAY_CODE,
                enter   = fadeIn(), exit    = fadeOut(),
            ) {
                PairingTopBar(onBackClick = onBackClick)
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (pairingState) {
                PairingState.DISPLAY_CODE -> DisplayCodeBody(
                    pairingToken = pairingToken,
                    countdown    = countdown,
                    onSimulateConnect = {
                        coroutineScope.launch {
                            pairingState = PairingState.CONNECTING
                            delay(2400L)
                            pairingState = PairingState.SUCCESS
                        }
                    },
                )

                PairingState.CONNECTING -> ConnectingBody()

                PairingState.SUCCESS -> SuccessBody(
                    circleScale   = circleScale.value,
                    checkProgress = checkProgress.value,
                    textAlpha     = textAlpha.value,
                    buttonAlpha   = buttonAlpha.value,
                    onContinue    = onPairingSuccess,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairingTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Connect to Caregiver", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.NavyBlue) },
        navigationIcon = {
            IconButton(onClick  = onBackClick, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = ElderlyColors.NavyBlue, modifier = Modifier.size(28.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ElderlyColors.SafeBackground),
    )
}

@Composable
private fun DisplayCodeBody(
    pairingToken:      String,
    countdown:         Int,
    onSimulateConnect: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waiting_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.35f, targetValue   = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1100, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "pulse_alpha",
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        InstructionCard()
        TokenCard(pairingToken = pairingToken)
        QrViewport(pairingToken = pairingToken, countdown = countdown)
        WaitingFooter(pulseAlpha = pulseAlpha)

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSimulateConnect, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, ElderlyColors.GrayBorder),
        ) {
            Text(text = "[DEV] Simulate Successful Connection", fontSize = 13.sp, color = ElderlyColors.SecondaryText)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InstructionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ElderlyColors.NavyLight),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElderlyColors.NavyBlue.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "📱", fontSize = 28.sp, modifier = Modifier.padding(top = 2.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(text = "Show this code to your caregiver", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.NavyBlue, lineHeight = 22.sp)
                Text(text = "Your caregiver needs to scan the QR code or type the code below to connect the devices.", fontSize = 14.sp, color = ElderlyColors.SecondaryText, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun TokenCard(pairingToken: String) {
    val safeToken = pairingToken.padEnd(6).take(6).uppercase()
    val chunkA    = safeToken.take(3)
    val chunkB    = safeToken.drop(3)

    Card(
        modifier  = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = ElderlyColors.Surface),
        border    = androidx.compose.foundation.BorderStroke(1.5.dp, ElderlyColors.GrayBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Device Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ElderlyColors.SecondaryText, letterSpacing = 1.sp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Chunk
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    TokenChunk(text = chunkA)
                }

                // Divider Dots
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(ElderlyColors.GrayBorder)) }
                }

                // Right Chunk
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    TokenChunk(text = chunkB)
                }
            }
            Text(text = "Share this code with your caregiver", fontSize = 12.sp, color = ElderlyColors.SecondaryText.copy(alpha = 0.65f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TokenChunk(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ElderlyColors.TokenBg)
            .border(width = 1.5.dp, color = ElderlyColors.NavyBlue.copy(alpha = 0.18f), shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // ── 🟢 FIXED: Lowered letter spacing and added singleLine constraints to prevent layout wrapping overlap
        Text(
            text          = text,
            fontSize      = 34.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = ElderlyColors.PrimaryText,
            letterSpacing = 2.sp,
            maxLines      = 1,
            softWrap      = false
        )
    }
}

@Composable
private fun QrViewport(pairingToken: String, countdown: Int) {
    val dynamicQrBitmap = remember(pairingToken) { generateQrCodeBitmap(pairingToken) }

    Card(
        modifier  = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = ElderlyColors.Surface),
        border    = androidx.compose.foundation.BorderStroke(1.5.dp, ElderlyColors.GrayBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "Or scan the QR code", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ElderlyColors.SecondaryText)
            Box(
                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                    .border(width = 1.dp, color = ElderlyColors.GrayBorder, shape = RoundedCornerShape(12.dp))
                    .drawWithContent {
                        drawContent()
                        drawQrAlignmentCorners(color = ElderlyColors.NavyBlue, strokeWidth = 3.dp.toPx(), cornerSize = 24.dp.toPx(), radius = 4.dp.toPx())
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (dynamicQrBitmap != null) {
                    Image(bitmap = dynamicQrBitmap.asImageBitmap(), contentDescription = "Pairing Connection Matrix", modifier = Modifier.size(145.dp))
                } else {
                    QrPlaceholderMatrix()
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                CircularProgressIndicator(
                    progress = countdown / 60f,
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = ElderlyColors.NavyBlue,
                    trackColor = ElderlyColors.GrayBorder.copy(alpha = 0.3f)
                )
                Text(
                    text = "Code auto-refreshes in ${countdown}s",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElderlyColors.NavyBlue,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun generateQrCodeBitmap(content: String): Bitmap? {
    return try {
        val size = 512
        val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }
    } catch (e: Exception) { null }
}

private fun DrawScope.drawQrAlignmentCorners(color: Color, strokeWidth: Float, cornerSize: Float, radius: Float) {
    val padding = 10f
    // Top-left
    drawLine(color = color, start = Offset(x = padding, y = padding + radius), end = Offset(x = padding, y = padding + cornerSize), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(x = padding + radius, y = padding), end = Offset(x = padding + cornerSize, y = padding), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    // Top-right
    val r = size.width
    drawLine(color = color, start = Offset(x = r - padding, y = padding + radius), end = Offset(x = r - padding, y = padding + cornerSize), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(x = r - padding - radius, y = padding), end = Offset(x = r - padding - cornerSize, y = padding), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    // Bottom-left
    val b = size.height
    drawLine(color = color, start = Offset(x = padding, y = b - padding - radius), end = Offset(x = padding, y = b - padding - cornerSize), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(x = padding + radius, y = b - padding), end = Offset(x = padding + cornerSize, y = b - padding), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    // Bottom-right
    drawLine(color = color, start = Offset(x = r - padding, y = b - padding - radius), end = Offset(x = r - padding, y = b - padding - cornerSize), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(x = r - padding - radius, y = b - padding), end = Offset(x = r - padding - cornerSize, y = b - padding), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

@Composable
private fun QrPlaceholderMatrix() {
    val pattern = remember { listOf(listOf(1,1,1,0,1,1,1), listOf(1,0,1,0,1,0,1), listOf(1,1,1,1,1,1,1), listOf(0,1,0,0,0,1,0), listOf(1,1,1,1,1,1,1), listOf(1,0,1,0,1,0,1), listOf(1,1,1,0,1,1,1)) }
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        pattern.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { cell ->
                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(if (cell == 1) ElderlyColors.PrimaryText else Color.Transparent))
                }
            }
        }
    }
}

@Composable
private fun WaitingFooter(pulseAlpha: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).alpha(pulseAlpha).background(ElderlyColors.NavyBlue))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "Waiting for connection...", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ElderlyColors.SecondaryText.copy(alpha = pulseAlpha))
    }
}

@Composable
private fun ConnectingBody() {
    Box(modifier = Modifier.fillMaxSize().background(ElderlyColors.SafeBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(72.dp), color = ElderlyColors.NavyBlue, strokeWidth = 5.dp, trackColor = ElderlyColors.NavyBlue.copy(alpha = 0.15f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Connecting your account...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText, textAlign = TextAlign.Center)
                Text(text = "Please wait a moment.\nThis may take a few seconds.", fontSize = 14.sp, color = ElderlyColors.SecondaryText, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun SuccessBody(circleScale: Float, checkProgress: Float, textAlpha: Float, buttonAlpha: Float, onContinue: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(ElderlyColors.SafeBackground), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Box(modifier = Modifier.size(120.dp).scale(circleScale).clip(CircleShape).background(ElderlyColors.SafeGreenLight), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Successful", tint = ElderlyColors.SafeGreen, modifier = Modifier.size(60.dp).scale(checkProgress).alpha(checkProgress))
            }
            Column(modifier = Modifier.fillMaxWidth().alpha(textAlpha), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Connection Successful!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ElderlyColors.PrimaryText, textAlign = TextAlign.Center)
                Text(text = "Your device has been securely connected to your caregiver.", fontSize = 16.sp, color = ElderlyColors.SecondaryText, textAlign = TextAlign.Center, lineHeight = 23.sp)
                SuccessChip(text = "Caregiver: Connected ✓")
            }
            Button(
                onClick  = onContinue, modifier = Modifier.fillMaxWidth().height(58.dp).alpha(buttonAlpha),
                shape    = RoundedCornerShape(16.dp), colors   = ButtonDefaults.buttonColors(containerColor = ElderlyColors.NavyBlue, contentColor   = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
            ) { Text(text = "Continue to Dashboard", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SuccessChip(text: String) {
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(ElderlyColors.SafeGreenLight).border(width = 1.dp, color = ElderlyColors.SafeGreen.copy(alpha = 0.35f), shape = RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 8.dp)) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ElderlyColors.SafeGreen)
    }
}