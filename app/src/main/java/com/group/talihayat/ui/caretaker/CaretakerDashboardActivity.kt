package com.group.talihayat.ui.caretaker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group.talihayat.ui.theme.*

// ─────────────────────────────────────────────────
//  STATE MODEL
// ─────────────────────────────────────────────────

enum class ComponentState { NORMAL, ALERT }

// ─────────────────────────────────────────────────
//  SPRING SPECS
// ─────────────────────────────────────────────────

val PremiumSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessLow
)
val CrispSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness    = Spring.StiffnessMediumLow)
val ColorTween    = tween<Color>(durationMillis = 600, easing = FastOutSlowInEasing)
val FloatTween    = tween<Float>(durationMillis = 500, easing = FastOutSlowInEasing)
val DpTween       = tween<Dp>(durationMillis = 500,    easing = FastOutSlowInEasing)

// ─────────────────────────────────────────────────
//  MAIN DASHBOARD
// ─────────────────────────────────────────────────

class CaretakerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaliHayatTheme {
                CaretakerDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaretakerDashboardScreen() {
    var isLinked by remember { mutableStateOf(false) }
    var isCheckingStatus by remember { mutableStateOf(true) }
    var elderlyBattery by remember { mutableIntStateOf(100) }

    var appState by remember { mutableStateOf(ComponentState.NORMAL) }
    var showPrivacySecurity by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("Home") }
    var showEditProfile by remember { mutableStateOf(false) }

    // 🟢 Track Live Steps from Firebase
    var elderlySteps by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    val caretakerUid = FirebaseAuth.getInstance().currentUser?.uid

    // 🟢 Real-time Steps Sync Listener
    // ── 🟢 1. STARTUP MEMORY CHECK ──
    // Runs only once when the app opens to see if they are already paired
    LaunchedEffect(Unit) {
        val caretakerUid = FirebaseAuth.getInstance().currentUser?.uid
        if (caretakerUid != null) {
            database.child("users").child(caretakerUid).child("pairedElderlyUid")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            isLinked = true // Skip the QR scanner!
                        }
                        isCheckingStatus = false // Stop the loading spinner
                    }
                    override fun onCancelled(error: DatabaseError) {
                        isCheckingStatus = false
                    }
                })
        } else {
            isCheckingStatus = false
        }
    }

    // ── 🟢 2. LIVE DATA SYNC ──
    // Runs automatically whenever 'isLinked' becomes true (either from startup or scanning)
    // ── 🟢 2. LIVE DATA SYNC ──
    LaunchedEffect(isLinked) {
        if (isLinked) {
            val caretakerUid = FirebaseAuth.getInstance().currentUser?.uid
            if (caretakerUid != null) {
                database.child("users").child(caretakerUid).child("pairedElderlyUid")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val elderlyUid = snapshot.getValue(String::class.java)
                            if (elderlyUid != null) {

                                // Stream live steps
                                database.child("users").child(elderlyUid).child("steps_today")
                                    .addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(stepSnap: DataSnapshot) {
                                            elderlySteps = stepSnap.getValue(Int::class.java) ?: 0
                                        }
                                        override fun onCancelled(error: DatabaseError) {}
                                    })

                                // 🟢 NEW: Stream the live battery status from the connected elderly user!
                                database.child("users").child(elderlyUid).child("battery_level")
                                    .addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(batterySnap: DataSnapshot) {
                                            elderlyBattery = batterySnap.getValue(Int::class.java) ?: 100
                                        }
                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }
    }

    val transition = updateTransition(targetState = appState, label = "AppStateTransition")

    val accentColor by transition.animateColor(
        transitionSpec = { ColorTween }, label = "AccentColor"
    ) { state ->
        if (state == ComponentState.NORMAL) Teal else Crimson
    }

    val accentGlow by transition.animateColor(
        transitionSpec = { ColorTween }, label = "AccentGlow"
    ) { state ->
        if (state == ComponentState.NORMAL) GlowTeal else Crimson.copy(alpha = 0.25f)
    }

    val accentLight by transition.animateColor(
        transitionSpec = { ColorTween }, label = "AccentLight"
    ) { state ->
        if (state == ComponentState.NORMAL) TealLight else CrimsonLight
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        AnimatedContent(
            targetState = isLinked,
            transitionSpec = {
                val enterTransition = fadeIn(
                    animationSpec = tween(durationMillis = 450, delayMillis = 50, easing = LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )

                val exitTransition = fadeOut(
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.98f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )

                enterTransition togetherWith exitTransition
            },
            label = "PairingToDashboardTransition"
        ) { linked ->
            if (!linked) {
                LinkElderlyScreen(
                    onLinkSuccess = { isLinked = true }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AmbientBlob(accentColor = accentColor, transition = transition)

                    AnimatedContent(
                        targetState = showEditProfile,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally { it } + fadeIn(tween(300))) togetherWith
                                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(300)))
                            } else {
                                (slideInHorizontally { -it / 3 } + fadeIn(tween(300))) togetherWith
                                        (slideOutHorizontally { it } + fadeOut(tween(300)))
                            }
                        },
                        label = "ProfileTransition"
                    ) { isEditing ->
                        if (isEditing) {
                            EditProfileScreen(onBackClick = { showEditProfile = false })
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimatedContent(targetState = currentTab, label = "TabSwitch") { tab ->
                                    when (tab) {
                                        // 🟢 Pass the live synced steps to the UI
                                        "Home" -> CaretakerHomeScreen(elderlySteps = elderlySteps, elderlyBattery = elderlyBattery)
                                        "Reports" -> ReportsScreenFull()
                                        "Elder's Hub" -> PatientScreen()
                                        "Settings" -> SettingsScreen(
                                            onNavigateToEditProfile = { showEditProfile = true },
                                            onNavigateToPrivacySecurity = { showPrivacySecurity = true }
                                        )
                                    }
                                }

                                TaliBottomNavHub(
                                    currentTab = currentTab,
                                    onTabSelected = { currentTab = it },
                                    accentColor = accentColor,
                                    accentGlow  = accentGlow,
                                    appState    = appState,
                                    onSosClick  = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:999")
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier    = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showPrivacySecurity,
                            enter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(350)),
                            exit = slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        ) {
                            PrivacySecurityScreen(onBackClick = { showPrivacySecurity = false })
                        }
                    }

                    SlidingAlertSheet(
                        visible  = appState == ComponentState.ALERT,
                        onDismiss = { appState = ComponentState.NORMAL },
                        onFullScreen = { /* Handle navigation */ }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  AMBIENT BACKGROUND BLOB
// ─────────────────────────────────────────────────

@Composable
fun AmbientBlob(
    accentColor: Color,
    transition: Transition<ComponentState>
) {
    val blobAlpha by transition.animateFloat(
        transitionSpec = { tween(800, easing = FastOutSlowInEasing) }, label = "BlobAlpha"
    ) { if (it == ComponentState.ALERT) 0.07f else 0.04f }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color  = accentColor.copy(alpha = blobAlpha),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.9f, -size.height * 0.1f)
        )
        drawCircle(
            color  = accentColor.copy(alpha = blobAlpha * 0.6f),
            radius = size.width * 0.5f,
            center = Offset(-size.width * 0.1f, size.height * 0.7f)
        )
    }
}

// ─────────────────────────────────────────────────
//  TOP BAR
// ─────────────────────────────────────────────────

@Composable
fun TaliTopBar(
    transition     : Transition<ComponentState>,
    accentColor    : Color,
    appState       : ComponentState,
    onToggleState  : () -> Unit
) {
    val statusBgAlpha by transition.animateFloat(
        transitionSpec = { FloatTween }, label = "StatusBg"
    ) { if (it == ComponentState.ALERT) 1f else 0f }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text       = "TaliHayat",
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Navy,
                letterSpacing = (-0.5).sp
            )
            Text(
                text     = "Fall Detection System",
                fontSize = 12.sp,
                color    = GrayMuted,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Surface, CircleShape)
                    .border(1.dp, GrayLight, CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint   = if (appState == ComponentState.ALERT)
                        Crimson else Navy,
                    modifier = Modifier.size(22.dp)
                )
                if (appState == ComponentState.ALERT) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Crimson, CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .background(
                        color = accentColor,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onToggleState() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = if (appState == ComponentState.NORMAL) "Simulate Fall" else "Reset",
                    color = Color.White,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  STATUS OVERVIEW CARD  (Circular Ring)
// ─────────────────────────────────────────────────

@Composable
fun StatusOverviewCard(
    transition  : Transition<ComponentState>,
    accentColor : Color,
    accentLight : Color,
    accentGlow  : Color,
    appState    : ComponentState,
    modifier    : Modifier = Modifier
) {
    val ringProgress by transition.animateFloat(
        transitionSpec = { tween(900, easing = FastOutSlowInEasing) }, label = "RingProgress"
    ) { if (it == ComponentState.NORMAL) 1f else 0.72f }

    val cardScale by transition.animateFloat(
        transitionSpec = { spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow) },
        label = "CardScale"
    ) { if (it == ComponentState.NORMAL) 1f else 1.015f }

    val infinitePulse = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infinitePulse.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseScale"
    )
    val pulseAlpha by infinitePulse.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseAlpha"
    )

    PremiumCard(
        modifier = modifier.scale(cardScale)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(130.dp)
            ) {
                if (appState == ComponentState.ALERT) {
                    Box(
                        modifier = Modifier
                            .size((130 * pulseScale).dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        accentGlow.copy(alpha = pulseAlpha * 0.6f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val inset       = strokeWidth / 2
                    val oval        = Rect(inset, inset, size.width - inset, size.height - inset)

                    drawArc(
                        color       = accentColor.copy(alpha = 0.12f),
                        startAngle  = -90f,
                        sweepAngle  = 360f,
                        useCenter   = false,
                        topLeft     = oval.topLeft,
                        size        = oval.size,
                        style       = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color      = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress,
                        useCenter  = false,
                        topLeft    = oval.topLeft,
                        size       = oval.size,
                        style      = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (appState == ComponentState.NORMAL) "100%" else "⚠",
                        fontSize   = if (appState == ComponentState.NORMAL) 20.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = accentColor
                    )
                    Text(
                        text     = if (appState == ComponentState.NORMAL) "Secure" else "ALERT",
                        fontSize = 10.sp,
                        color    = accentColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (appState == ComponentState.NORMAL)
                        "All Systems Secure" else "Fall Detected!",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Navy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (appState == ComponentState.NORMAL)
                        "The elderly is safe and actively monitored via AI-powered sensors."
                    else
                        "Abnormal motion pattern detected. Immediate verification required.",
                    fontSize   = 12.sp,
                    color      = GrayMuted,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .background(accentLight, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(accentColor, CircleShape)
                        )
                        Text(
                            text       = if (appState == ComponentState.NORMAL) "Active Monitoring" else "Critical Alert",
                            fontSize   = 11.sp,
                            color      = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  LIVE MONITORING GRID
// ─────────────────────────────────────────────────

@Composable
fun LiveMonitoringGrid(
    transition  : Transition<ComponentState>,
    accentColor : Color,
    accentLight : Color,
    appState    : ComponentState,
    modifier    : Modifier = Modifier
) {
    val cardAlpha by transition.animateFloat(
        transitionSpec = { FloatTween }, label = "CardAlpha"
    ) { if (it == ComponentState.NORMAL) 1f else 0.92f }

    val cardScale by transition.animateFloat(
        transitionSpec = { spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow) },
        label = "GridScale"
    ) { if (it == ComponentState.NORMAL) 1f else 0.975f }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha; scaleX = cardScale; scaleY = cardScale },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AiCameraCard(
            accentColor = accentColor,
            accentLight = accentLight,
            appState    = appState,
            modifier    = Modifier.weight(1f)
        )

        ActivityStatusCard(
            accentColor = accentColor,
            accentLight = accentLight,
            appState    = appState,
            modifier    = Modifier.weight(1f)
        )
    }
}

@Composable
fun AiCameraCard(
    accentColor : Color,
    accentLight : Color,
    appState    : ComponentState,
    modifier    : Modifier = Modifier
) {
    val scanInfinite = rememberInfiniteTransition(label = "CameraScan")
    val scanOffset by scanInfinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ScanLine"
    )

    PremiumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = "AI Vision",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Navy
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = if (appState == ComponentState.NORMAL)
                                Teal else Crimson,
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Navy.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height * scanOffset
                    drawLine(
                        color       = accentColor.copy(alpha = 0.6f),
                        start       = Offset(0f, y),
                        end         = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                    val cols = 3; val rows = 3
                    for (i in 1 until cols) {
                        drawLine(
                            color  = accentColor.copy(alpha = 0.1f),
                            start  = Offset(size.width / cols * i, 0f),
                            end    = Offset(size.width / cols * i, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (i in 1 until rows) {
                        drawLine(
                            color  = accentColor.copy(alpha = 0.1f),
                            start  = Offset(0f, size.height / rows * i),
                            end    = Offset(size.width, size.height / rows * i),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    drawCircle(
                        color  = accentColor.copy(alpha = 0.25f),
                        radius = 18.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint     = accentColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )

                if (appState == ComponentState.ALERT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Crimson.copy(alpha = 0.10f))
                    )
                    Text(
                        text       = "FALL",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Crimson,
                        modifier   = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(CrimsonLight, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .background(accentLight, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = if (appState == ComponentState.NORMAL)
                        "Confidence: 98.4%" else "Fall Conf: 91.2%",
                    fontSize   = 10.sp,
                    color      = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ActivityStatusCard(
    accentColor : Color,
    accentLight : Color,
    appState    : ComponentState,
    modifier    : Modifier = Modifier
) {
    val activityLabel  = if (appState == ComponentState.NORMAL) "Walking"      else "Fall Detected"
    val activityEmoji  = if (appState == ComponentState.NORMAL) "🚶"           else "🚨"
    val lastSeen       = if (appState == ComponentState.NORMAL) "Just now"     else "Unresponsive"
    val lastSeenColor  = if (appState == ComponentState.NORMAL) Teal else Crimson

    val barHeights = if (appState == ComponentState.NORMAL)
        listOf(0.3f, 0.5f, 0.4f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f)
    else
        listOf(0.9f, 0.95f, 1.0f, 0.2f, 0.05f, 0.0f, 0.0f, 0.0f)

    val pulseInfinite = rememberInfiniteTransition(label = "ActivityPulse")
    val pulseAlpha by pulseInfinite.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseDot"
    )

    PremiumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = "Activity",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Navy
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = accentColor.copy(alpha = pulseAlpha),
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState  = activityLabel,
                transitionSpec = {
                    fadeIn(tween(350)) + scaleIn(
                        initialScale  = 0.88f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                    ) togetherWith fadeOut(tween(200))
                },
                label = "ActivityBadge"
            ) { label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentLight, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = activityEmoji, fontSize = 22.sp)
                    Column {
                        Text(
                            text       = label,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = accentColor
                        )
                        Text(
                            text     = lastSeen,
                            fontSize = 10.sp,
                            color    = lastSeenColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text     = "Last 8 hrs",
                fontSize = 10.sp,
                color    = GrayMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                barHeights.forEach { heightFraction ->
                    val barColor by animateColorAsState(
                        targetValue   = if (heightFraction > 0.85f && appState == ComponentState.ALERT)
                            Crimson else accentColor.copy(alpha = 0.6f + heightFraction * 0.4f),
                        animationSpec = tween(500),
                        label         = "SparkBar"
                    )
                    val animatedHeight by animateFloatAsState(
                        targetValue   = heightFraction,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        label         = "SparkHeight"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((36 * animatedHeight).dp)
                            .background(barColor, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────
//  QUICK STATS ROW
// ─────────────────────────────────────────────────

@Composable
fun QuickStatsRow(
    accentColor : Color,
    accentLight : Color,
    modifier    : Modifier = Modifier
) {
    val stats = listOf(
        Triple("Heart Rate", "72 bpm", Icons.Filled.Favorite),
        Triple("Battery",    "87%",    Icons.Filled.BatteryFull),
        Triple("Signal",     "Strong", Icons.Filled.SignalCellularAlt)
    )

    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { (label, value, icon) ->
            PremiumCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier              = Modifier.padding(14.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = accentColor,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text       = value,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Navy
                    )
                    Text(
                        text     = label,
                        fontSize = 9.sp,
                        color    = GrayMuted,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  BOTTOM NAVIGATION HUB
// ─────────────────────────────────────────────────

@Composable
fun TaliBottomNavHub(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    accentColor: Color,
    accentGlow: Color,
    appState: ComponentState,
    onSosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = Surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .shadow(
                elevation = 16.dp,
                shape     = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = CardShadow,
                spotColor    = CardShadow
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(Icons.Filled.Home, "Home", currentTab == "Home", accentColor) { onTabSelected("Home") }
            NavItem(Icons.Filled.Analytics, "Reports", currentTab == "Reports", accentColor) { onTabSelected("Reports") }

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(Crimson, Crimson.copy(alpha = 0.8f))
                            ),
                            shape = CircleShape
                        )
                        .clickable { onSosClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SOS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
            }

            NavItem(Icons.Filled.Person, "Elder's Hub", currentTab == "Elder's Hub", accentColor) { onTabSelected("Elder's Hub") }
            NavItem(Icons.Filled.Settings, "Settings", currentTab == "Settings", accentColor) { onTabSelected("Settings") }
        }
    }
}

@Composable
fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, accentColor: Color, onClick: () -> Unit) {
    val tint = if (isSelected) accentColor else GrayMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(text = label, fontSize = 9.sp, color = tint, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────
//  SLIDING ALERT SHEET
// ─────────────────────────────────────────────────

@Composable
fun SlidingAlertSheet(
    visible      : Boolean,
    onDismiss    : () -> Unit,
    onFullScreen : () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(
            initialOffsetY  = { it },
            animationSpec   = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(300)),
        exit    = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(onClick = onDismiss)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(
            initialOffsetY  = { it },
            animationSpec   = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow
            )
        ),
        exit    = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Surface,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(GrayLight, RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(CrimsonLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Warning,
                            contentDescription = null,
                            tint               = Crimson,
                            modifier           = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text       = "🚨 Critical Fall Detected!",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Navy
                        )
                        Text(
                            text     = "Immediate response required",
                            fontSize = 13.sp,
                            color    = GrayMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrimsonLight, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlertDetailRow(label = "Location", value = "Living Room — Sensor Node 3")
                        AlertDetailRow(label = "Time",     value = "Just now  •  High confidence")
                        AlertDetailRow(label = "Severity", value = "Critical  •  No movement detected")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick  = onFullScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Crimson,
                        contentColor   = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Videocam,
                        contentDescription = null,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text       = "GO FULL SCREEN CAMERA",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, GrayLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GrayMuted
                    )
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Close,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text       = "CANCEL FALSE ALARM",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AlertDetailRow(label: String, value: String) {
    Row {
        Text(
            text       = "$label: ",
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Crimson
        )
        Text(
            text     = value,
            fontSize = 12.sp,
            color    = Navy
        )
    }
}

// ─────────────────────────────────────────────────
//  SHARED PREMIUM CARD COMPONENT
// ─────────────────────────────────────────────────

@Composable
fun PremiumCard(
    modifier  : Modifier = Modifier,
    content   : @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.shadow(
            elevation    = 8.dp,
            shape        = RoundedCornerShape(24.dp),
            ambientColor = CardShadow,
            spotColor    = CardShadow
        ),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}