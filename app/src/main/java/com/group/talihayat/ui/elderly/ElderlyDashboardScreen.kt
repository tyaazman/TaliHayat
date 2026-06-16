package com.group.talihayat.ui.elderly

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.group.talihayat.ui.theme.*
import androidx.compose.foundation.BorderStroke
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// 🟢 NEW: Data model for notifications
data class AppNotification(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val timeString: String = "",
    val timestamp: Long = 0L,
    val iconType: String = "info"
)

enum class ElderlyUiState {
    MONITORING,
    CAMERA_OFFLINE,
    FALL_COUNTDOWN,
    HELP_SENT
}

@Composable
fun ElderlyDashboardScreen(
    userName: String,
    caregivers: List<CaregiverData>,
    uiState              : ElderlyUiState,
    countdown            : Int,
    countdownDuration    : Int,
    fallSensitivity      : Float,
    batteryLevel         : Int,
    cloudSynced          : Boolean,
    cameraOnline         : Boolean,
    stepsCount           : Int,
    onCancel             : () -> Unit,
    onSimulate           : () -> Unit,
    onCountdownChange    : (Int) -> Unit,
    onFallSensitivityChange: (Float) -> Unit,
    onLogout             : () -> Unit
) {
    var currentTab by remember { mutableStateOf("Home") }
    var globalFontSize by remember { mutableStateOf(AppFontSize.Normal) }
    var globalHighContrast by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    // 🟢 Read Live Notifications from Firebase
    val database = FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val notificationsList = remember { mutableStateListOf<AppNotification>() }

    LaunchedEffect(currentUid) {
        if (currentUid != null) {
            database.child("users").child(currentUid).child("notifications")
                .orderByChild("timestamp")
                .limitToLast(15) // Keep it clean by only pulling the last 15 updates
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        notificationsList.clear()
                        for (child in snapshot.children) {
                            val id = child.key ?: ""
                            val title = child.child("title").getValue(String::class.java) ?: ""
                            val desc = child.child("description").getValue(String::class.java) ?: ""
                            val time = child.child("timeString").getValue(String::class.java) ?: ""
                            val ts = child.child("timestamp").getValue(Long::class.java) ?: 0L
                            val icon = child.child("iconType").getValue(String::class.java) ?: "info"
                            notificationsList.add(AppNotification(id, title, desc, time, ts, icon))
                        }
                        notificationsList.sortByDescending { it.timestamp } // Put newest at the top
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElderlyColors.Background)
    ) {

        // ── SCREEN ROUTING CONTAINER WITH SMOOTH TRANSITIONS ─────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val enterTransition = fadeIn(animationSpec = tween(durationMillis = 280, delayMillis = 40, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.93f, animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing))
                    val exitTransition = fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.97f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                    enterTransition togetherWith exitTransition
                },
                label = "tab_navigation_animation"
            ) { targetTab ->
                when (targetTab) {
                    "Home" -> {
                        StableDashboard(
                            userName      = userName,
                            caregivers    = caregivers,
                            uiState       = uiState,
                            batteryLevel  = batteryLevel,
                            cloudSynced   = cloudSynced,
                            cameraOnline  = cameraOnline,
                            stepsCount    = stepsCount,
                            onSimulate    = onSimulate,
                            onAvatarClick = { currentTab = "Settings" },
                            fontSize      = globalFontSize,
                            onNotificationClick = { showNotifications = true }
                        )
                    }
                    "Medical Hub" -> { MedicalHubScreen(fontSize = globalFontSize) }
                    "Settings" -> {
                        ElderlySettingsScreen(
                            fontSize = globalFontSize,
                            highContrast = globalHighContrast,
                            countdownDuration = countdownDuration,
                            fallSensitivity = fallSensitivity,
                            onFontSizeChange = { globalFontSize = it },
                            onHighContrastChange = { selectedValue -> globalHighContrast = selectedValue; com.group.talihayat.ui.theme.isHighContrastModeActive = selectedValue },
                            onCountdownDurationChange = onCountdownChange,
                            onFallSensitivityChange = onFallSensitivityChange,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }

        if (uiState != ElderlyUiState.FALL_COUNTDOWN && uiState != ElderlyUiState.HELP_SENT) {
            ElderlyBottomNavHub(activeTab = currentTab, onTabSelect = { currentTab = it }, modifier = Modifier.align(Alignment.BottomCenter))
        }

        AnimatedVisibility(
            visible = uiState == ElderlyUiState.FALL_COUNTDOWN,
            enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) { FallCountdownOverlay(countdown = countdown, onCancel = onCancel) }

        AnimatedVisibility(
            visible = uiState == ElderlyUiState.HELP_SENT,
            enter   = fadeIn() + scaleIn(initialScale = 1.05f),
            exit    = fadeOut()
        ) { HelpSentOverlay() }

        // ── LIVE NOTIFICATIONS MODAL CENTER OVERLAY ───────────────────────────────
        // ── LIVE NOTIFICATIONS MODAL CENTER OVERLAY WITH SWIPE & CLEAR ALL ─────────────────
        if (showNotifications) {
            AlertDialog(
                onDismissRequest = { showNotifications = false },
                containerColor   = ElderlyColors.Surface,
                shape            = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Notifications, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(26.dp))
                            Text(text = "Recent Updates", fontSize = 20.scaled(globalFontSize), fontWeight = FontWeight.ExtraBold, color = ElderlyColors.NavyBlue)
                        }

                        // 🟢 NEW: Clear All Button!
                        if (notificationsList.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    currentUid?.let { uid ->
                                        // Wipes out all notification records for this user from Firebase
                                        database.child("users").child(uid).child("notifications").removeValue()
                                    }
                                }
                            ) {
                                Text(text = "Clear All", fontSize = 14.scaled(globalFontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.Crimson)
                            }
                        }
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()).padding(vertical = 4.dp)
                    ) {
                        if (notificationsList.isEmpty()) {
                            Text("No recent updates at the moment.", fontSize = 14.scaled(globalFontSize), color = ElderlyColors.GrayMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
                        } else {
                            notificationsList.forEachIndexed { index, notif ->
                                val icon = when(notif.iconType) {
                                    "medication" -> Icons.Filled.Healing
                                    "alert" -> Icons.Filled.FlashOn
                                    "caregiver" -> Icons.Filled.AccountCircle
                                    else -> Icons.Filled.Notifications
                                }

                                // 🟢 NEW: Slide to Delete Wrapper
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { newValue ->
                                        if (newValue == SwipeToDismissBoxValue.EndToStart) {
                                            currentUid?.let { uid ->
                                                // Deletes this specific item from Firebase when swiped away!
                                                database.child("users").child(uid).child("notifications").child(notif.id).removeValue()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false, // Only allow swiping left
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(ElderlyColors.Crimson, RoundedCornerShape(14.dp))
                                                .padding(end = 16.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Alert", tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                ) {
                                    // Wrap the actual row item so it has a clean background container while swiping
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = ElderlyColors.Surface
                                    ) {
                                        Column {
                                            NotificationRowItem(
                                                icon = icon,
                                                title = notif.title,
                                                time = notif.timeString,
                                                description = notif.description,
                                                fontSize = globalFontSize
                                            )
                                            if (index < notificationsList.size - 1) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                HorizontalDivider(color = ElderlyColors.GrayLight, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showNotifications = false }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = ElderlyColors.NavyBlue)) {
                        Text(text = "Close", fontSize = 15.scaled(globalFontSize), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun StableDashboard(
    userName     : String,
    caregivers   : List<CaregiverData>,
    uiState      : ElderlyUiState,
    batteryLevel : Int,
    cloudSynced  : Boolean,
    cameraOnline : Boolean,
    stepsCount   : Int,
    onSimulate   : () -> Unit,
    onAvatarClick: () -> Unit,
    fontSize     : AppFontSize,
    onNotificationClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(ElderlyColors.Background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 36.dp)) {
            Spacer(Modifier.height(54.dp))

            TopProfileHeader(name = userName, onSimulate = onSimulate, onAvatarClick = onAvatarClick, fontSize = fontSize, onNotificationClick = onNotificationClick, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(28.dp))

            ProtectionHubCard(cameraOnline = cameraOnline, fontSize = fontSize, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(16.dp))
            StepsTrackerCard(stepsCount = stepsCount, fontSize = fontSize, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(16.dp))
            DeviceAnalyticsGrid(batteryLevel = batteryLevel, cloudSynced = cloudSynced, fontSize = fontSize, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(16.dp))
            CaregiverHubCard(caregivers = caregivers, fontSize = fontSize, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(24.dp))
            SafetyTipsFooter(fontSize = fontSize, modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
private fun StepsTrackerCard(stepsCount: Int, fontSize: AppFontSize, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(46.dp).background(ElderlyColors.NavySurface, CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Filled.DirectionsWalk, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "STEPS TAKEN TODAY", fontSize = 11.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.SecondaryText, letterSpacing = 1.sp)
                Spacer(Modifier.height(2.dp))
                Text(text = "$stepsCount steps", fontSize = 22.scaled(fontSize), fontWeight = FontWeight.ExtraBold, color = ElderlyColors.PrimaryText)
                Text(text = "Keep up the wonderful work!", fontSize = 12.scaled(fontSize), color = ElderlyColors.HeartbeatGreen, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ProtectionHubCard(cameraOnline: Boolean, fontSize: AppFontSize, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("MY SAFETY STATUS", fontSize = 11.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.SecondaryText, letterSpacing = 1.sp)
            Spacer(Modifier.height(18.dp))
            ProtectionRow(label = "Home Safety Camera", status = if (cameraOnline) "Connected & Monitoring" else "Camera Disconnected", isActive = cameraOnline, icon = if (cameraOnline) Icons.Filled.Videocam else Icons.Filled.VideocamOff, fontSize = fontSize)
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), thickness = 0.5.dp, color = ElderlyColors.GrayBorder.copy(alpha = 0.5f))
            ProtectionRow(label = "Phone Fall Detector", status = "Active & Monitoring Movements", isActive = true, icon = Icons.Filled.Sensors, fontSize = fontSize)
        }
    }
}

@Composable
private fun ProtectionRow(label: String, status: String, isActive: Boolean, icon: ImageVector, fontSize: AppFontSize) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(42.dp).background(if (isActive) ElderlyColors.NavySurface else ElderlyColors.GrayBorder.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isActive) ElderlyColors.NavyBlue else ElderlyColors.SecondaryText, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
            Text(status, fontSize = 12.scaled(fontSize), color = if (isActive) ElderlyColors.HeartbeatGreen else ElderlyColors.SecondaryText, fontWeight = FontWeight.Medium)
        }
        if (isActive) { Box(modifier = Modifier.size(8.dp).background(ElderlyColors.HeartbeatGreen, CircleShape)) }
    }
}

@Composable
private fun DeviceAnalyticsGrid(batteryLevel: Int, cloudSynced: Boolean, fontSize: AppFontSize, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AnalyticsItem(label = "Battery", value = "$batteryLevel%", icon = Icons.Filled.BatteryFull, color = ElderlyColors.HeartbeatGreen, fontSize = fontSize, modifier = Modifier.weight(1f))
        AnalyticsItem(label = "Caregiver Connection", value = if (cloudSynced) "Connected Safely" else "Connection Error", icon = if (cloudSynced) Icons.Filled.Cloud else Icons.Filled.CloudOff, color = ElderlyColors.NavyBlue, fontSize = fontSize, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AnalyticsItem(label: String, value: String, icon: ImageVector, color: Color, fontSize: AppFontSize, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(18.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, fontSize = 12.scaled(fontSize), color = ElderlyColors.SecondaryText)
            Text(value, fontSize = 20.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
        }
    }
}

@Composable
private fun CaregiverHubCard(caregivers: List<CaregiverData>, fontSize: AppFontSize, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(22.dp)) {
            Text("MY CAREGIVER(S)", fontSize = 11.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.SecondaryText)
            Spacer(Modifier.height(16.dp))
            if (caregivers.isEmpty()) {
                Text("No caregivers linked yet.", fontSize = 14.scaled(fontSize), color = ElderlyColors.GrayMuted)
            } else {
                caregivers.forEachIndexed { index, caregiver ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(54.dp).background(ElderlyColors.NavySurface, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Filled.AccountBox, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(28.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(caregiver.name.ifBlank { "Caregiver" }, fontSize = 18.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
                            Text("Caregiver", fontSize = 13.scaled(fontSize), color = ElderlyColors.SecondaryText)
                        }
                        IconButton(
                            onClick = { if (caregiver.phone.isNotBlank()) context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${caregiver.phone}"))) },
                            modifier = Modifier.size(48.dp).background(ElderlyColors.NavyBlue, CircleShape)
                        ) { Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(22.dp)) }
                    }
                    if (index < caregivers.size - 1) { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = ElderlyColors.GrayBorder) }
                }
            }
        }
    }
}

@Composable
private fun SafetyTipsFooter(fontSize: AppFontSize, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("TODAY'S SAFETY TIPS", fontSize = 11.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.SecondaryText)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.DirectionsWalk, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Move slowly when getting up from a chair.", fontSize = 13.scaled(fontSize), color = ElderlyColors.SecondaryText)
            }
        }
    }
}

@Composable
private fun FallCountdownOverlay(countdown: Int, onCancel: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(ElderlyColors.DangerBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = ElderlyColors.CancelYellow, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("FALL DETECTED", color = Color.White.copy(0.7f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Are You Okay?", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            Text("$countdown", fontSize = 100.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("seconds", color = Color.White.copy(0.7f))
            Spacer(Modifier.height(60.dp))
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = ElderlyColors.CancelYellow)) {
                Text("I AM OKAY", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun HelpSentOverlay() {
    Box(modifier = Modifier.fillMaxSize().background(ElderlyColors.SentBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Filled.LocalHospital, contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(24.dp))
            Text("HELP IS\nON THE WAY.", fontSize = 44.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 50.sp)
            Spacer(Modifier.height(16.dp))
            Text("Emergency contacts notified.", color = Color.White.copy(0.7f))
        }
    }
}

@Composable
private fun ElderlyBottomNavHub(activeTab: String, onTabSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().height(84.dp), color = ElderlyColors.Surface, shadowElevation = 16.dp, border = BorderStroke(1.dp, ElderlyColors.GrayBorder.copy(alpha = 0.6f))) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
            val items = listOf(Triple("Home", Icons.Filled.Shield, Icons.Outlined.Shield), Triple("Medical Hub", Icons.Filled.LocalHospital, Icons.Outlined.LocalHospital), Triple("Settings", Icons.Filled.Settings, Icons.Outlined.Settings))
            items.forEach { (title, filledIcon, outlinedIcon) ->
                val isSelected = activeTab == title
                val targetColor = if (isSelected) ElderlyColors.NavyBlue else ElderlyColors.SecondaryText
                Column(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTabSelect(title) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(imageVector = if (isSelected) filledIcon else outlinedIcon, contentDescription = title, tint = targetColor, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(text = title, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, color = targetColor)
                }
            }
        }
    }
}

@Composable
private fun TopProfileHeader(name: String, onSimulate: () -> Unit, onAvatarClick: () -> Unit, fontSize: AppFontSize, onNotificationClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(58.dp).background(ElderlyColors.NavySurface, CircleShape).border(2.dp, ElderlyColors.NavyBlue.copy(0.1f), CircleShape).clickable { onAvatarClick() }, contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(30.dp))
            }
            Column {
                Text(text = "Hello", fontSize = 13.scaled(fontSize), color = ElderlyColors.SecondaryText)
                Text(text = "$name!", fontSize = 22.scaled(fontSize), fontWeight = FontWeight.ExtraBold, color = ElderlyColors.PrimaryText)
            }
        }
        IconButton(onClick = onNotificationClick, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "View Alerts Panel", tint = ElderlyColors.NavyBlue, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun NotificationRowItem(icon: ImageVector, title: String, time: String, description: String, fontSize: AppFontSize) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(40.dp).background(ElderlyColors.NavySurface, CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 14.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
                Text(text = time, fontSize = 11.scaled(fontSize), color = ElderlyColors.GrayMuted)
            }
            Spacer(Modifier.height(2.dp))
            Text(text = description, fontSize = 12.scaled(fontSize), color = ElderlyColors.SecondaryText, lineHeight = 16.sp)
        }
    }
}

fun Int.scaled(tier: AppFontSize): androidx.compose.ui.unit.TextUnit {
    val multiplier = when (tier) { AppFontSize.Normal -> 1.00f; AppFontSize.Large -> 1.25f; AppFontSize.ExtraLarge -> 1.50f }
    return (this * multiplier).sp
}