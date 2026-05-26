package com.group.talihayat

import android.os.Bundle
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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

class CaretakerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CaretakerDashboardTheme {
                CaretakerDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaretakerDashboardScreen() {
    var appState by remember { mutableStateOf(ComponentState.NORMAL) }
    val transition = updateTransition(targetState = appState, label = "AppStateTransition")

    val accentColor by transition.animateColor(
        transitionSpec = { ColorTween }, label = "AccentColor"
    ) { state ->
        if (state == ComponentState.NORMAL) TaliColors.TealSafe else TaliColors.CrimsonAlert
    }

    val accentGlow by transition.animateColor(
        transitionSpec = { ColorTween }, label = "AccentGlow"
    ) { state ->
        if (state == ComponentState.NORMAL) TaliColors.GlowTeal else TaliColors.GlowCrimson
    }

    val accentLight by transition.animateColor(
        transitionSpec = { ColorTween }, label = "AccentLight"
    ) { state ->
        if (state == ComponentState.NORMAL) TaliColors.TealSafeLight else TaliColors.CrimsonLight
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TaliColors.Background)
    ) {
        AmbientBlob(accentColor = accentColor, transition = transition)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp)
        ) {
            CaretakerTopBar(
                accentColor = accentColor,
                appState = appState,
                onToggleState = {
                    appState = if (appState == ComponentState.NORMAL)
                        ComponentState.ALERT else ComponentState.NORMAL
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            CaretakerStatusCard(
                transition = transition,
                accentColor = accentColor,
                accentLight = accentLight,
                accentGlow = accentGlow,
                appState = appState,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            CaretakerMonitoringGrid(
                transition = transition,
                accentColor = accentColor,
                accentLight = accentLight,
                appState = appState,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            CaretakerQuickStats(
                accentColor = accentColor,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        CaretakerBottomNav(
            accentColor = accentColor,
            accentGlow = accentGlow,
            onSosClick = { appState = ComponentState.ALERT },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        CaretakerAlertSheet(
            visible = appState == ComponentState.ALERT,
            onDismiss = { appState = ComponentState.NORMAL },
            onFullScreen = { /* Handle navigation */ }
        )
    }
}

@Composable
private fun AmbientBlob(
    accentColor: Color,
    transition: Transition<ComponentState>
) {
    val blobAlpha by transition.animateFloat(
        transitionSpec = { tween(800, easing = FastOutSlowInEasing) }, label = "BlobAlpha"
    ) { if (it == ComponentState.ALERT) 0.07f else 0.04f }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = accentColor.copy(alpha = blobAlpha),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.9f, -size.height * 0.1f)
        )
        drawCircle(
            color = accentColor.copy(alpha = blobAlpha * 0.6f),
            radius = size.width * 0.5f,
            center = Offset(-size.width * 0.1f, size.height * 0.7f)
        )
    }
}

@Composable
private fun CaretakerTopBar(
    accentColor: Color,
    appState: ComponentState,
    onToggleState: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "TaliHayat",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TaliColors.Navy,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Fall Detection System",
                fontSize = 12.sp,
                color = TaliColors.GrayMuted,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(TaliColors.Surface, CircleShape)
                    .border(1.dp, TaliColors.GrayLight, CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = if (appState == ComponentState.ALERT)
                        TaliColors.CrimsonAlert else TaliColors.Navy,
                    modifier = Modifier.size(22.dp)
                )
                if (appState == ComponentState.ALERT) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(TaliColors.CrimsonAlert, CircleShape)
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
                    text = if (appState == ComponentState.NORMAL) "Simulate Fall" else "Reset",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CaretakerStatusCard(
    transition: Transition<ComponentState>,
    accentColor: Color,
    accentLight: Color,
    accentGlow: Color,
    appState: ComponentState,
    modifier: Modifier = Modifier
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
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseScale"
    )
    val pulseAlpha by infinitePulse.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseAlpha"
    )

    CaretakerPremiumCard(
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
                modifier = Modifier.size(130.dp)
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
                    val inset = strokeWidth / 2
                    val oval = Rect(inset, inset, size.width - inset, size.height - inset)

                    drawArc(
                        color = accentColor.copy(alpha = 0.12f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = oval.topLeft,
                        size = oval.size,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress,
                        useCenter = false,
                        topLeft = oval.topLeft,
                        size = oval.size,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (appState == ComponentState.NORMAL) "100%" else "⚠",
                        fontSize = if (appState == ComponentState.NORMAL) 20.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                    Text(
                        text = if (appState == ComponentState.NORMAL) "Secure" else "ALERT",
                        fontSize = 10.sp,
                        color = accentColor,
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaliColors.Navy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (appState == ComponentState.NORMAL)
                        "Patient is safe and actively monitored via AI-powered sensors."
                    else
                        "Abnormal motion pattern detected. Immediate verification required.",
                    fontSize = 12.sp,
                    color = TaliColors.GrayMuted,
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
                            text = if (appState == ComponentState.NORMAL) "Active Monitoring" else "Critical Alert",
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaretakerMonitoringGrid(
    transition: Transition<ComponentState>,
    accentColor: Color,
    accentLight: Color,
    appState: ComponentState,
    modifier: Modifier = Modifier
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
        CaretakerAiCameraCard(
            accentColor = accentColor,
            accentLight = accentLight,
            appState = appState,
            modifier = Modifier.weight(1f)
        )

        CaretakerActivityStatusCard(
            accentColor = accentColor,
            accentLight = accentLight,
            appState = appState,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CaretakerAiCameraCard(
    accentColor: Color,
    accentLight: Color,
    appState: ComponentState,
    modifier: Modifier = Modifier
) {
    val scanInfinite = rememberInfiniteTransition(label = "CameraScan")
    val scanOffset by scanInfinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ScanLine"
    )

    CaretakerPremiumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "AI Vision",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaliColors.Navy
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = if (appState == ComponentState.NORMAL)
                                TaliColors.TealSafe else TaliColors.CrimsonAlert,
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(TaliColors.Navy.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height * scanOffset
                    drawLine(
                        color = accentColor.copy(alpha = 0.6f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                    val cols = 3; val rows = 3
                    for (i in 1 until cols) {
                        drawLine(
                            color = accentColor.copy(alpha = 0.1f),
                            start = Offset(size.width / cols * i, 0f),
                            end = Offset(size.width / cols * i, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (i in 1 until rows) {
                        drawLine(
                            color = accentColor.copy(alpha = 0.1f),
                            start = Offset(0f, size.height / rows * i),
                            end = Offset(size.width, size.height / rows * i),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    drawCircle(
                        color = accentColor.copy(alpha = 0.25f),
                        radius = 18.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )

                if (appState == ComponentState.ALERT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TaliColors.CrimsonAlert.copy(alpha = 0.10f))
                    )
                    Text(
                        text = "FALL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TaliColors.CrimsonAlert,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(TaliColors.CrimsonLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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
                    text = if (appState == ComponentState.NORMAL)
                        "Confidence: 98.4%" else "Fall Conf: 91.2%",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CaretakerActivityStatusCard(
    accentColor: Color,
    accentLight: Color,
    appState: ComponentState,
    modifier: Modifier = Modifier
) {
    val activityLabel = if (appState == ComponentState.NORMAL) "Walking" else "Fall Detected"
    val activityEmoji = if (appState == ComponentState.NORMAL) "🚶" else "🚨"
    val lastSeen = if (appState == ComponentState.NORMAL) "Just now" else "Unresponsive"
    val lastSeenColor = if (appState == ComponentState.NORMAL) TaliColors.TealSafe else TaliColors.CrimsonAlert

    val lastActiveText = if (appState == ComponentState.NORMAL) "2 mins ago" else "5 mins ago"
    val fallsToday = if (appState == ComponentState.NORMAL) "0" else "1"
    val stepsToday = if (appState == ComponentState.NORMAL) "1,240" else "1,240"

    val pulseInfinite = rememberInfiniteTransition(label = "ActivityPulse")
    val pulseAlpha by pulseInfinite.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseDot"
    )

    CaretakerPremiumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaliColors.Navy
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(accentColor.copy(alpha = pulseAlpha), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = activityLabel,
                transitionSpec = {
                    fadeIn(tween(350)) + scaleIn(
                        initialScale = 0.88f,
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = activityEmoji, fontSize = 20.sp)
                    Column {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                        Text(
                            text = lastSeen,
                            fontSize = 10.sp,
                            color = lastSeenColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                Triple("⏱", "Last active", lastActiveText),
                Triple("🦺", "Falls today", fallsToday),
                Triple("👣", "Steps today", stepsToday)
            ).forEach { (emoji, label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = emoji, fontSize = 12.sp)
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = TaliColors.GrayMuted
                        )
                    }
                    Text(
                        text = value,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (label == "Falls today" && value != "0")
                            TaliColors.CrimsonAlert else TaliColors.Navy
                    )
                }
                if (label != "Steps today") {
                    HorizontalDivider(color = TaliColors.GrayLight, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun CaretakerQuickStats(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val stats = listOf(
        Triple("Battery", "87%", Icons.Filled.BatteryFull),
        Triple("Signal", "Strong", Icons.Filled.SignalCellularAlt)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { (label, value, icon) ->
            CaretakerPremiumCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TaliColors.Navy
                    )
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = TaliColors.GrayMuted,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CaretakerBottomNav(
    accentColor: Color,
    accentGlow: Color,
    onSosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sosInfinite = rememberInfiniteTransition(label = "SosPulse")
    val sosGlow by sosInfinite.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "SosGlow"
    )
    val sosScale by sosInfinite.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "SosScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = TaliColors.Surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = TaliColors.CardShadow,
                spotColor = TaliColors.CardShadow
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CaretakerNavItem(icon = Icons.Filled.Home, label = "Home", tint = accentColor)
            CaretakerNavItem(icon = Icons.Filled.Analytics, label = "Reports", tint = TaliColors.GrayMuted)

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size((70 * sosScale).dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    accentGlow.copy(alpha = sosGlow),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.8f))
                            ),
                            shape = CircleShape
                        )
                        .clickable { onSosClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SOS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            CaretakerNavItem(icon = Icons.Filled.Settings, label = "Settings", tint = TaliColors.GrayMuted)
            CaretakerNavItem(icon = Icons.Filled.Person, label = "Patient", tint = TaliColors.GrayMuted)
        }
    }
}

@Composable
private fun CaretakerNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { }
            .padding(horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(text = label, fontSize = 9.sp, color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CaretakerAlertSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onFullScreen: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
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
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = TaliColors.Surface,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(TaliColors.GrayLight, RoundedCornerShape(2.dp))
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
                            .background(TaliColors.CrimsonLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = TaliColors.CrimsonAlert,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "🚨 Critical Fall Detected!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TaliColors.Navy
                        )
                        Text(
                            text = "Immediate response required",
                            fontSize = 13.sp,
                            color = TaliColors.GrayMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TaliColors.CrimsonLight, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CaretakerAlertDetailRow(label = "Location", value = "Living Room — Sensor Node 3")
                        CaretakerAlertDetailRow(label = "Time", value = "Just now  •  High confidence")
                        CaretakerAlertDetailRow(label = "Severity", value = "Critical  •  No movement detected")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onFullScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaliColors.CrimsonAlert,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GO FULL SCREEN CAMERA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, TaliColors.GrayLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TaliColors.GrayMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CANCEL FALSE ALARM",
                        fontSize = 13.sp,
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
private fun CaretakerAlertDetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TaliColors.CrimsonAlert
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = TaliColors.Navy
        )
    }
}

@Composable
private fun CaretakerPremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(24.dp),
            ambientColor = TaliColors.CardShadow,
            spotColor = TaliColors.CardShadow
        ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = TaliColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun CaretakerDashboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

// ── SHARED RESOURCES (Local to this file to avoid conflicts) ──

private object TaliColors {
    val Background    = Color(0xFFF8F9FA)
    val Surface       = Color(0xFFFFFFFF)
    val TealSafe      = Color(0xFF00A8B5)
    val TealSafeLight = Color(0xFFE0F7FA)
    val CrimsonAlert  = Color(0xFFFF5757)
    val CrimsonLight  = Color(0xFFFFEBEB)
    val Navy          = Color(0xFF1E3A5F)
    val GrayMuted     = Color(0xFF8A9BB0)
    val GrayLight     = Color(0xFFECEFF1)
    val GlowTeal      = Color(0x4000A8B5)
    val GlowCrimson   = Color(0x40FF5757)
    val CardShadow    = Color(0x14000000)
}

private enum class ComponentState { NORMAL, ALERT }
private val ColorTween = tween<Color>(durationMillis = 600, easing = FastOutSlowInEasing)
private val FloatTween = tween<Float>(durationMillis = 500, easing = FastOutSlowInEasing)
