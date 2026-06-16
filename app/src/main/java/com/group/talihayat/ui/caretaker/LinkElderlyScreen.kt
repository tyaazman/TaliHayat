package com.group.talihayat.ui.caretaker

// ═══════════════════════════════════════════════════════════════════════════════
//  LinkElderlyScreen.kt
//  Onboarding handshake screen — Caregiver links to Elderly Dependent's device.
// ═══════════════════════════════════════════════════════════════════════════════

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity // 🟢 Added to explicitly bind parent lifecycles
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import com.group.talihayat.ui.theme.*
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin

private enum class LinkMode { TYPING, SCANNER, SUCCESS }

private val CircleSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
private val CheckSpring  = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
private val TextSpring   = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkElderlyScreen(onLinkSuccess: () -> Unit) {

    var mode          by remember { mutableStateOf(LinkMode.TYPING) }
    var token         by remember { mutableStateOf("") }
    var tokenError    by remember { mutableStateOf<String?>(null) }
    var showPassword  by remember { mutableStateOf(false) }

    val circleScale   = remember { Animatable(0f) }
    val checkScale    = remember { Animatable(0f) }
    val bannerAlpha   = remember { Animatable(0f) }
    val bannerOffsetY = remember { Animatable(20f) }

    val context = LocalContext.current
    val database = remember {
        FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    }
    val caretakerUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            mode = LinkMode.SCANNER
        } else {
            tokenError = "Camera permission is required to scan QR codes"
        }
    }

    val processHandshakePairing: (String) -> Unit = { scannedToken ->
        val cleanToken = scannedToken.uppercase().trim()
        if (caretakerUid != null) {
            database.child("pairing_tokens").child(cleanToken)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val elderlyUid = snapshot.child("elderlyUid").getValue(String::class.java)
                        if (elderlyUid != null) {
                            // 🟢 FIXED: Caregiver gets 1 Elderly string. Elderly gets a MAP of Caregivers!
                            database.child("users").child(caretakerUid).child("pairedElderlyUid").setValue(elderlyUid)
                            database.child("users").child(elderlyUid).child("caregivers").child(caretakerUid).setValue(true)

                            database.child("pairing_tokens").child(cleanToken).child("status").setValue("linked")
                            mode = LinkMode.SUCCESS
                        } else {
                            tokenError = "Invalid or expired device pairing token"
                            mode = LinkMode.TYPING
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        tokenError = "Database connectivity error"
                        mode = LinkMode.TYPING
                    }
                })
        }
    }

    LaunchedEffect(mode) {
        if (mode != LinkMode.SUCCESS) return@LaunchedEffect
        circleScale.snapTo(0f)
        checkScale.snapTo(0f)
        bannerAlpha.snapTo(0f)
        bannerOffsetY.snapTo(20f)

        delay(320)
        circleScale.animateTo(1f, CircleSpring)
        delay(150)
        checkScale.animateTo(1.1f, CheckSpring)
        checkScale.animateTo(1.0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        delay(180)
        bannerAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        bannerOffsetY.animateTo(0f, TextSpring)
        delay(1800)
        onLinkSuccess()
    }

    Box(
        modifier         = Modifier.fillMaxSize().background(if (mode == LinkMode.SCANNER) Color.Black else LBackground),
        contentAlignment = Alignment.Center
    ) {
        if (mode == LinkMode.TYPING) { AmbientBackground() }

        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                when (targetState) {
                    LinkMode.TYPING  -> (slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300))) togetherWith (slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(200)))
                    LinkMode.SCANNER -> fadeIn(tween(280)) togetherWith fadeOut(tween(200))
                    LinkMode.SUCCESS -> fadeIn(tween(280)) togetherWith fadeOut(tween(300))
                }
            },
            label = "ModeSwitch"
        ) { currentMode ->
            when (currentMode) {
                LinkMode.TYPING -> TypingEntryContent(
                    token = token,
                    onTokenChange = { if (it.length <= 6) { token = it.uppercase(); tokenError = null } },
                    tokenError = tokenError,
                    showPassword = showPassword,
                    onTogglePass = { showPassword = !showPassword },
                    onConnect = {
                        if (token.length < 6) tokenError = "Token must be exactly 6 characters"
                        else processHandshakePairing(token)
                    },
                    onScanQr = {
                        if (hasCameraPermission) mode = LinkMode.SCANNER
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                LinkMode.SCANNER -> LiveQrScannerView(
                    onClose = { mode = LinkMode.TYPING },
                    onQrCodeDetected = { rawToken ->
                        if (rawToken.length == 6) {
                            processHandshakePairing(rawToken)
                        }
                    }
                )

                LinkMode.SUCCESS -> PairingSuccessContent(
                    circleScale = circleScale.value, checkScale = checkScale.value,
                    bannerAlpha = bannerAlpha.value, bannerOffsetY = bannerOffsetY.value
                )
            }
        }
    }
}

@Composable
private fun LiveQrScannerView(
    onClose: () -> Unit,
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    // 🟢 FIXED: Extract parent activity reference directly to handle lifecycles safely
    val activity = remember(context) { context as ComponentActivity }

    val laser = rememberInfiniteTransition(label = "Laser")
    val laserY by laser.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "LaserY"
    )

    val density = LocalDensity.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isScanningActive by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analyzer ->
                            analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                                @SuppressLint("UnsafeOptInUsageError")
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && isScanningActive) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                                    val options = BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                        .build()
                                    val scanner = BarcodeScanning.getClient(options)

                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue ?: ""
                                                if (rawValue.length == 6) {
                                                    isScanningActive = false
                                                    onQrCodeDetected(rawValue)
                                                    break
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        // 🟢 FIXED: Bound components directly to the extracted parent Activity context stability map
                        cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageAnalyzer)
                    } catch (e: Exception) {
                        Log.e("TaliHayat", "Camera configuration runtime fault", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // HUD Overlay Architecture
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxW  = size.width * 0.65f
            val boxH  = boxW
            val left  = (size.width  - boxW) / 2f
            val top   = (size.height - boxH) / 2f
            val right = left + boxW
            val bot   = top  + boxH
            val rx    = with(density) { 20.dp.toPx() }

            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(RoundRect(Rect(left, top, right, bot), CornerRadius(rx, rx)))
            }
            drawPath(path, color = Color.Black.copy(alpha = 0.65f), style = Fill)

            val arm   = with(density) { 28.dp.toPx() }
            val sw    = with(density) { 3.5.dp.toPx() }
            val white = Color.White

            drawLine(white, Offset(left, top + rx), Offset(left, top + arm + rx), sw, StrokeCap.Round)
            drawLine(white, Offset(left + rx, top), Offset(left + arm + rx, top), sw, StrokeCap.Round)
            drawLine(white, Offset(right, top + rx), Offset(right, top + arm + rx), sw, StrokeCap.Round)
            drawLine(white, Offset(right - rx, top), Offset(right - arm - rx, top), sw, StrokeCap.Round)
            drawLine(white, Offset(left, bot - rx), Offset(left, bot - arm - rx), sw, StrokeCap.Round)
            drawLine(white, Offset(left + rx, bot), Offset(left + arm + rx, bot), sw, StrokeCap.Round)
            drawLine(white, Offset(right, bot - rx), Offset(right, bot - arm - rx), sw, StrokeCap.Round)
            drawLine(white, Offset(right - rx, bot), Offset(right - arm - rx, bot), sw, StrokeCap.Round)

            val laserActualY = top + laserY * boxH
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(LCrimsonAlert.copy(alpha = 0f), LCrimsonAlert, LCrimsonAlert.copy(alpha = 0f)),
                    startX = left, endX = right
                ),
                start = Offset(left + rx * 0.5f, laserActualY), end = Offset(right - rx * 0.5f, laserActualY),
                strokeWidth = with(density) { 2.dp.toPx() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Box(modifier = Modifier.size(38.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, "Close scanner", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Scan QR Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(38.dp))
        }

        Text(
            text = "Align the QR code inside the frame",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  DESIGN TOKENS & SUB-COMPOSABLES (Preserved)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AmbientBackground() {
    val drift = rememberInfiniteTransition(label = "Drift")
    val driftAngle by drift.animateFloat(
        initialValue  = 0f, targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "DriftAngle"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rad = driftAngle * (Math.PI / 180f).toFloat()
        drawCircle(
            color  = LTealSafe.copy(alpha = 0.07f), radius = size.width * 0.70f,
            center = Offset(size.width * 0.9f + cos(rad) * size.width * 0.06f, -size.height * 0.05f + sin(rad) * size.height * 0.04f)
        )
        drawCircle(color  = LNavy.copy(alpha = 0.04f), radius = size.width * 0.50f, center = Offset(-size.width * 0.08f, size.height * 0.92f))
    }
}

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
        modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(72.dp).background(LTealLight, CircleShape).border(2.dp, LTealSafe.copy(alpha = 0.35f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Filled.Link, contentDescription = null, tint = LTealSafe, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(text = "Connect Your\nLoved One", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = LNavy, textAlign = TextAlign.Center, lineHeight = 37.sp, letterSpacing = (-0.5).sp)
        Spacer(Modifier.height(10.dp))
        Text(text = "Enter the 6-character pairing token shown on your elderly family member's phone, or scan the QR code on their screen.", fontSize = 14.sp, color = LGrayMuted, textAlign = TextAlign.Center, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.height(36.dp))

        LinkCard {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(text = "Pairing Token", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (tokenError != null) LCrimsonAlert else LGrayMuted, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value         = token,
                    onValueChange = onTokenChange,
                    placeholder   = { Text("e.g.  K79 B2X", fontSize = 14.sp, color    = LGrayMuted.copy(alpha = 0.6f), letterSpacing = 1.sp) },
                    leadingIcon   = { Icon(imageVector = Icons.Filled.VpnKey, contentDescription = null, tint = if (tokenError != null) LCrimsonAlert else LTealSafe, modifier = Modifier.size(20.dp)) },
                    trailingIcon  = { IconButton(onClick = onScanQr) { Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = "Scan QR code", tint = LTealSafe, modifier = Modifier.size(22.dp)) } },
                    visualTransformation = if (!showPassword) PasswordVisualTransformation('•') else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    isError       = tokenError != null, singleLine    = true, modifier      = Modifier.fillMaxWidth(), shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = LTealSafe, unfocusedBorderColor = LGrayLight, errorBorderColor     = LCrimsonAlert, cursorColor          = LTealSafe),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = LNavy)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = tokenError != null, enter = expandVertically(tween(200)) + fadeIn(tween(150)), exit = shrinkVertically(tween(180)) + fadeOut(tween(120))) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = LCrimsonAlert, modifier = Modifier.size(13.dp))
                            Text(text = tokenError.orEmpty(), fontSize = 11.sp, color = LCrimsonAlert, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(text = "${token.length} / 6", fontSize = 11.sp, color = if (token.length == 6) LTealSafe else LGrayMuted)
                }

                TextButton(onClick = onTogglePass, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null, tint = LGrayMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(text = if (showPassword) "Hide token" else "Show token", fontSize = 12.sp, color = LGrayMuted)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth().background(LNavyLight, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Info, null, tint = LNavy, modifier = Modifier.size(16.dp).padding(top = 1.dp))
            Text(text = "Ask your elderly family member to open TaliHayat on their phone and tap \"Show My Code\". The 6-character token will appear on their screen.", fontSize = 12.sp, color = LNavy, lineHeight = 18.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(brush = Brush.horizontalGradient(listOf(LTealSafe, LTealDark)), shape = RoundedCornerShape(17.dp))
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(17.dp), ambientColor = LTealSafe.copy(alpha = 0.25f), spotColor = LTealSafe.copy(alpha = 0.25f)).clickable { onConnect() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Link, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(text = "Establish Connection", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.3.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onScanQr, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.5.dp, LTealSafe.copy(alpha = 0.55f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = LTealSafe)) {
            Icon(Icons.Outlined.QrCodeScanner, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text = "Scan QR Code Instead", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PairingSuccessContent(circleScale: Float, checkScale: Float, bannerAlpha: Float, bannerOffsetY: Float) {
    val halo = rememberInfiniteTransition(label = "HaloGlow")
    val haloAlpha by halo.animateFloat(initialValue = 0.25f, targetValue = 0.70f, animationSpec = infiniteRepeatable(animation = tween(1100, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "HaloAlpha")
    val haloScale by halo.animateFloat(initialValue = 1.0f, targetValue = 1.18f, animationSpec = infiniteRepeatable(animation = tween(1100, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "HaloScale")
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize().background(LBackground), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(color = LTealSafe.copy(alpha = 0.06f), radius = size.width * 0.75f, center = Offset(size.width / 2f, size.height / 2f)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                Canvas(modifier = Modifier.size(220.dp * haloScale * circleScale)) { drawCircle(brush = Brush.radialGradient(colors = listOf(LSuccessGreen.copy(alpha = haloAlpha * 0.4f), LSuccessGreen.copy(alpha = haloAlpha * 0.15f), Color.Transparent))) }
                Canvas(modifier = Modifier.size(160.dp * circleScale)) {
                    val stroke = with(density) { 4.dp.toPx() }
                    drawCircle(color = LSuccessLight, radius = size.minDimension / 2f)
                    drawCircle(color = LSuccessGreen.copy(alpha = haloAlpha), radius = size.minDimension / 2f - stroke / 2f, style = Stroke(width = stroke))
                }
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Pairing successful", tint = LSuccessGreen, modifier = Modifier.size((80 * checkScale).dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale; alpha = checkScale })
            }
            Spacer(Modifier.height(32.dp))
            Box(modifier = Modifier.graphicsLayer { alpha = bannerAlpha; translationY = with(density) { bannerOffsetY.dp.toPx() } }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Connection Established\nSecurely", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = LNavy, textAlign = TextAlign.Center, lineHeight = 31.sp, letterSpacing = (-0.3).sp)
                    Spacer(Modifier.height(10.dp))
                    Text(text = "Device is now securely linked.\nFall monitoring will begin shortly.", fontSize = 14.sp, color = LGrayMuted, textAlign = TextAlign.Center, lineHeight = 21.sp)
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SuccessChip(label = "Encrypted"); SuccessChip(label = "Verified"); SuccessChip(label = "Monitoring On")
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), ambientColor = LCardShadow, spotColor = LCardShadow), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = LSurface), elevation = CardDefaults.cardElevation(0.dp)) { Column(content = content) }
}

@Composable
private fun SuccessChip(label: String) {
    Box(modifier = Modifier.background(LSuccessLight, RoundedCornerShape(20.dp)).border(1.dp, LSuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) { Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
}