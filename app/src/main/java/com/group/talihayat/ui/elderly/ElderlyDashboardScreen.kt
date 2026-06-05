package com.group.talihayat.ui.elderly

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

private object ElderlyColors {
    val SafeBackground   = Color(0xFFF8F9FA)
    val DangerBackground = Color(0xFFD32F2F)
    val SentBackground   = Color(0xFF1B5E20)
    val PrimaryText      = Color(0xFF1A1A1A)
    val SecondaryText    = Color(0xFF616161)
    val HeartbeatGreen   = Color(0xFF00C853)
    val CancelYellow     = Color(0xFFFFD600)
    val NavyBlue         = Color(0xFF1E3A5F)
    val Surface          = Color(0xFFFFFFFF)
    val NavySurface      = Color(0xFFE8EEF6)
    val GrayBorder       = Color(0xFFE0E0E0)
}

enum class ElderlyUiState {
    MONITORING,       // Stable — sensor active, camera online
    CAMERA_OFFLINE,   // Camera heartbeat lost — IMU still running
    FALL_COUNTDOWN,   // Fall triggered — 15s countdown overlay
    HELP_SENT         // Timer expired — alert confirmed sent
}

@Composable
fun ElderlyDashboardScreen(
    uiState      : ElderlyUiState,
    countdown    : Int,
    batteryLevel : Int,
    cloudSynced  : Boolean,
    cameraOnline : Boolean, // Tracks independent camera status
    onCancel     : () -> Unit,
    onSimulate   : () -> Unit 
) {
    Box(modifier = Modifier.fillMaxSize()) {
        StableDashboard(
            uiState      = uiState,
            batteryLevel = batteryLevel,
            cloudSynced  = cloudSynced,
            cameraOnline = cameraOnline,
            onSimulate   = onSimulate
        )

        AnimatedVisibility(
            visible = uiState == ElderlyUiState.FALL_COUNTDOWN,
            enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            FallCountdownOverlay(countdown = countdown, onCancel = onCancel)
        }

        AnimatedVisibility(
            visible = uiState == ElderlyUiState.HELP_SENT,
            enter   = fadeIn() + scaleIn(initialScale = 1.05f),
            exit    = fadeOut()
        ) {
            HelpSentOverlay()
        }
    }
}

@Composable
private fun StableDashboard(
    uiState      : ElderlyUiState,
    batteryLevel : Int,
    cloudSynced  : Boolean,
    cameraOnline : Boolean,
    onSimulate   : () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(ElderlyColors.SafeBackground)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 36.dp)) {
            Spacer(Modifier.height(54.dp))
            TopProfileHeader(onSimulate = onSimulate, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(28.dp))
            
            // Redesigned Card: Seperates Camera and Phone Sensor
            ProtectionHubCard(cameraOnline = cameraOnline, modifier = Modifier.padding(horizontal = 24.dp))
            
            Spacer(Modifier.height(16.dp))
            DeviceAnalyticsGrid(batteryLevel = batteryLevel, cloudSynced = cloudSynced, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(16.dp))
            CaregiverHubCard(modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(24.dp))
            SafetyTipsFooter(modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
private fun TopProfileHeader(onSimulate: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(58.dp).background(ElderlyColors.NavySurface, CircleShape).border(2.dp, ElderlyColors.NavyBlue.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Text("👴", fontSize = 26.sp)
            }
            Column {
                Text(
                    text = "Good Morning!",
                    fontSize = 13.sp,
                    color = ElderlyColors.SecondaryText,
                    modifier = Modifier.clickable { onSimulate() }
                )
                Text(text = "Saleha Sulaiman", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ElderlyColors.PrimaryText)
            }
        }
        Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, tint = ElderlyColors.NavyBlue)
    }
}

@Composable
private fun ProtectionHubCard(cameraOnline: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("PROTECTION HUB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.SecondaryText, letterSpacing = 1.sp)
            Spacer(Modifier.height(18.dp))

            // Row 1: AI Room Camera
            ProtectionRow(
                label    = "AI Room Camera",
                status   = if (cameraOnline) "Online & Watching" else "Camera Offline",
                isActive = cameraOnline,
                icon     = if (cameraOnline) Icons.Filled.Videocam else Icons.Filled.VideocamOff
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), thickness = 0.5.dp, color = ElderlyColors.GrayBorder.copy(alpha = 0.5f))

            // Row 2: Phone Fall Sensor
            ProtectionRow(
                label    = "Phone Fall Sensor",
                status   = "IMU Protection Active",
                isActive = true, 
                icon     = Icons.Filled.Sensors
            )
        }
    }
}

@Composable
private fun ProtectionRow(label: String, status: String, isActive: Boolean, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier.size(42.dp).background(if (isActive) ElderlyColors.NavySurface else ElderlyColors.GrayBorder.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) ElderlyColors.NavyBlue else ElderlyColors.SecondaryText,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
            Text(status, fontSize = 12.sp, color = if (isActive) ElderlyColors.HeartbeatGreen else ElderlyColors.SecondaryText, fontWeight = FontWeight.Medium)
        }
        if (isActive) {
            Box(modifier = Modifier.size(8.dp).background(ElderlyColors.HeartbeatGreen, CircleShape))
        }
    }
}

@Composable
private fun DeviceAnalyticsGrid(batteryLevel: Int, cloudSynced: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AnalyticsItem(label = "Battery", value = "$batteryLevel%", icon = Icons.Filled.BatteryFull, color = ElderlyColors.HeartbeatGreen, modifier = Modifier.weight(1f))
        AnalyticsItem(label = "Cloud Sync", value = if (cloudSynced) "Connected" else "Offline", icon = if (cloudSynced) Icons.Filled.Cloud else Icons.Filled.CloudOff, color = ElderlyColors.NavyBlue, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AnalyticsItem(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(18.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, fontSize = 12.sp, color = ElderlyColors.SecondaryText)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
        }
    }
}

@Composable
private fun CaregiverHubCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(22.dp)) {
            Text("MY CAREGIVER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.SecondaryText)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(60.dp).background(ElderlyColors.NavySurface, CircleShape), contentAlignment = Alignment.Center) {
                    Text("👩‍⚕️", fontSize = 28.sp)
                }
                Column {
                    Text("Nur Fatiyah", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
                    Text("Primary Caregiver", fontSize = 13.sp, color = ElderlyColors.SecondaryText)
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = ElderlyColors.NavyBlue)) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Call Caregiver", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SafetyTipsFooter(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("DAILY SAFETY TIPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.SecondaryText)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🚶", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Text("Move slowly when getting up from a chair.", fontSize = 13.sp, color = ElderlyColors.SecondaryText)
            }
        }
    }
}

@Composable
private fun FallCountdownOverlay(countdown: Int, onCancel: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(ElderlyColors.DangerBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚠️ FALL DETECTED", color = Color.White.copy(0.7f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
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
            Text("🚑", fontSize = 80.sp)
            Spacer(Modifier.height(24.dp))
            Text("HELP IS\nON THE WAY.", fontSize = 44.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 50.sp)
            Spacer(Modifier.height(16.dp))
            Text("Emergency contacts notified.", color = Color.White.copy(0.7f))
        }
    }
}
