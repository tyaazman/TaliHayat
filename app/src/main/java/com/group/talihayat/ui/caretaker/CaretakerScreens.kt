package com.group.talihayat.ui.caretaker

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.group.talihayat.ui.theme.*

// ─────────────────────────────────────────────────
//  SHARED SECTION HEADER
// ─────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text          = title.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        color         = GrayMuted,
        letterSpacing = 1.5.sp,
        modifier      = modifier.padding(bottom = 10.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  1. REPORTS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryTile(
    label   : String,
    value   : String,
    icon    : ImageVector,
    color   : Color,
    bgColor : Color,
    modifier : Modifier = Modifier
) {
    TaliCard(modifier = modifier) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text       = value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Navy
            )
            Text(
                text      = label,
                fontSize  = 11.sp,
                color     = GrayMuted,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(text = label, fontSize = 10.sp, color = GrayMuted)
    }
}

@Composable
private fun WeeklyBarChart(
    days     : List<String>,
    steps    : List<Float>,
    falls    : List<Int>,
    modifier : Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val barCount   = days.size
            val totalWidth = size.width
            val barWidth   = (totalWidth / barCount) * 0.45f
            val gap        = (totalWidth / barCount) * 0.55f
            val maxBarH    = size.height * 0.85f

            days.indices.forEach { i ->
                val barH     = maxBarH * steps[i] * animProgress.value
                val x        = i * (barWidth + gap) + gap / 2
                val barTop   = size.height - barH
                val hasFall  = falls[i] > 0

                drawRoundRect(
                    color     = Color.Black.copy(alpha = 0.05f),
                    topLeft   = Offset(x + 2f, barTop + 4f),
                    size      = Size(barWidth, barH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors     = if (hasFall)
                            listOf(Crimson, Crimson.copy(alpha = 0.65f))
                        else listOf(Teal, Teal.copy(alpha = 0.55f)),
                        startY = barTop,
                        endY   = size.height
                    ),
                    topLeft      = Offset(x, barTop),
                    size         = Size(barWidth, barH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                if (hasFall) {
                    drawCircle(
                        color  = Crimson,
                        radius = 5.dp.toPx(),
                        center = Offset(x + barWidth / 2f, barTop - 8.dp.toPx())
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEachIndexed { i, day ->
                Text(
                    text      = day,
                    fontSize  = 11.sp,
                    color     = if (falls[i] > 0) Crimson else GrayMuted,
                    fontWeight = if (falls[i] > 0) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IncidentItem(
    time        : String,
    description : String,
    severity    : String,
    date        : String
) {
    val (iconVec, iconColor, bgColor) = when (severity) {
        "critical" -> Triple(Icons.Filled.Warning,           Crimson, CrimsonLight)
        "warning"  -> Triple(Icons.Filled.NotificationImportant, AmberWarning,  Color(0xFFFFF8E1))
        else       -> Triple(Icons.Filled.CheckCircle,        Teal,    TealLight)
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(bgColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconVec, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = description,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Navy
            )
            Text(
                text     = "$date · $time",
                fontSize = 12.sp,
                color    = GrayMuted
            )
        }

        Box(
            modifier = Modifier
                .background(bgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text       = severity.replaceFirstChar { it.uppercase() },
                fontSize   = 10.sp,
                color      = iconColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ReportsScreenFull() {
    data class Incident(
        val time: String, val description: String,
        val severity: String, val date: String
    )

    val incidents = listOf(
        Incident("2:14 PM", "Fall Detected — Living Room",  "critical", "Today"),
        Incident("9:05 AM", "Low Battery Warning (18%)",    "warning",  "Today"),
        Incident("8:00 AM", "System Online — All Sensors",  "info",     "Today"),
        Incident("7:48 PM", "Fall Detected — Bathroom",     "critical", "Yesterday"),
        Incident("3:20 PM", "Camera Reconnected",           "info",     "Yesterday"),
        Incident("11:30 AM","Motion: Extended Inactivity",  "warning",  "Yesterday"),
        Incident("6:00 AM", "Daily Health Check Passed",    "info",     "2 days ago")
    )

    val days  = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    val steps = listOf(0.45f, 0.72f, 0.38f, 0.88f, 0.60f, 0.30f, 0.55f)
    val falls = listOf(0, 0, 1, 0, 0, 0, 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Reports", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
            Text("Week of June 2–8, 2025", fontSize = 13.sp, color = GrayMuted)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier              = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryTile("Falls",     "2", Icons.Filled.Warning,         Crimson, CrimsonLight, Modifier.weight(1f))
            SummaryTile("Alerts",    "5", Icons.Filled.Notifications,   AmberWarning, Color(0xFFFFF8E1),       Modifier.weight(1f))
            SummaryTile("Safe Days", "5", Icons.Filled.HealthAndSafety, Teal,     TealLight,    Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        TaliCard(modifier = Modifier.padding(horizontal = 24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Weekly Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Text("Steps & fall events", fontSize = 12.sp, color = GrayMuted)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendDot(Teal,    "Activity")
                        LegendDot(Crimson,"Fall")
                    }
                }
                Spacer(Modifier.height(20.dp))
                WeeklyBarChart(days = days, steps = steps, falls = falls, modifier = Modifier.fillMaxWidth().height(140.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Incident History")
            TaliCard {
                Column {
                    incidents.forEachIndexed { index, inc ->
                        IncidentItem(
                            time        = inc.time,
                            description = inc.description,
                            severity    = inc.severity,
                            date        = inc.date
                        )
                        if (index < incidents.lastIndex) {
                            HorizontalDivider(
                                color     = GrayBorder,
                                thickness = 0.5.dp,
                                modifier  = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  2. PATIENT SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PatientScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Navy, Navy.copy(alpha = 0.85f))
                    )
                )
                .padding(top = 48.dp, bottom = 32.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color  = Teal.copy(alpha = 0.12f),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.85f, 0f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(TealLight, CircleShape)
                            .border(3.dp, Teal.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👴", fontSize = 40.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(GreenOnline, CircleShape)
                            .border(2.dp, Navy, CircleShape)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text       = "Dato' Ahmad Razali",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
                Text(
                    text     = "82 years old  ·  Male",
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.65f)
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PatientTag(label = "🛡 Sensor Armed", color = Teal)
                    PatientTag(label = "🔋 Battery 85%",  color = GreenOnline)
                    PatientTag(label = "📡 Online",        color = Color(0xFF5B6EF5))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier              = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTile("72",        "BPM",         "Heart Rate", Crimson, Modifier.weight(1f))
            MetricTile("98.2°F",    "",             "Temperature", AmberWarning, Modifier.weight(1f))
            MetricTile("0",         "Falls",        "Today",      Teal,     Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Emergency Contacts")
            TaliCard {
                Column {
                    val contacts = listOf(
                        Triple("Siti Nurhaliza",    "Primary Caregiver",   "+60 12-345 6789"),
                        Triple("Dr. Hafiz Rahman",  "Physician",           "+60 3-2691 0000"),
                        Triple("Ahmad Jr.",          "Son (Next of Kin)",   "+60 11-234 5678")
                    )
                    contacts.forEachIndexed { i, (name, role, phone) ->
                        ContactRow(name = name, role = role, phone = phone)
                        if (i < contacts.lastIndex) {
                            HorizontalDivider(color = GrayBorder, thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Medical Profile")
            TaliCard {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MedicalRow(Icons.Filled.Bloodtype,   "Blood Type",              "B+")
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    MedicalRow(Icons.Filled.Warning,     "Allergies",               "Penicillin, Shellfish")
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    MedicalRow(Icons.Filled.LocalHospital,"Underlying Conditions",  "Hypertension, Type 2 Diabetes")
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    MedicalRow(Icons.Filled.Medication,  "Current Medications",     "Metformin 500mg, Amlodipine 5mg")
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    MedicalRow(Icons.Filled.CalendarMonth,"Last Check-up",          "15 May 2025")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Last Known Location")
            TaliCard {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(NavySurface, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = Navy, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Living Room", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Text("Last detected 3 minutes ago", fontSize = 12.sp, color = GrayMuted)
                    }
                    Box(
                        modifier = Modifier
                            .background(TealLight, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("LIVE", fontSize = 10.sp, color = Teal, fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetricTile(
    value    : String,
    unit     : String,
    label    : String,
    color    : Color,
    modifier : Modifier = Modifier
) {
    TaliCard(modifier = modifier) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
                if (unit.isNotEmpty()) {
                    Text(unit, fontSize = 11.sp, color = color, modifier = Modifier.padding(bottom = 3.dp, start = 2.dp))
                }
            }
            Text(
                text      = label,
                fontSize  = 11.sp,
                color     = GrayMuted,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContactRow(name: String, role: String, phone: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(NavySurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = name.first().toString(),
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Navy
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Text(role, fontSize = 12.sp, color = GrayMuted)
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(TealLight, CircleShape)
                .clickable { /* trigger phone intent */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Call, "Call $name", tint = Teal, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MedicalRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = GrayMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = GrayMuted, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, color = Navy, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  3. SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(onNavigateToEditProfile: () -> Unit) {
    var pushNotifications by remember { mutableStateOf(true) }
    var smsAlerts         by remember { mutableStateOf(true) }
    var emailReports      by remember { mutableStateOf(false) }
    var sensitivity       by remember { mutableStateOf(0.65f) }

    val sensitivityLabel = when {
        sensitivity < 0.33f -> "Low"
        sensitivity < 0.67f -> "Medium"
        else                -> "High"
    }
    val sensitivityColor = when (sensitivityLabel) {
        "Low"    -> GreenOnline
        "Medium" -> AmberWarning
        else     -> Crimson
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
            Text("Manage your TaliHayat configuration", fontSize = 13.sp, color = GrayMuted)
        }

        Spacer(Modifier.height(28.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Account")
            TaliCard {
                Column {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(NavySurface, CircleShape)
                                .border(2.dp, Teal.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👩‍⚕️", fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Siti Nurhaliza", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                            Text("Primary Caregiver", fontSize = 12.sp, color = GrayMuted)
                        }
                    }

                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)

                    SettingsNavRow(
                        icon      = Icons.Outlined.Edit,
                        label     = "Edit Profile",
                        subtitle  = "Update your name, photo & password",
                        onClick   = onNavigateToEditProfile
                    )

                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)

                    SettingsNavRow(
                        icon     = Icons.Outlined.Security,
                        label    = "Privacy & Security",
                        subtitle = "Two-factor auth, data sharing",
                        onClick  = { }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Preferences")
            TaliCard {
                Column {
                    SettingsToggleRow(
                        icon      = Icons.Outlined.Notifications,
                        label     = "Push Notifications",
                        subtitle  = "Instant alerts to this device",
                        checked   = pushNotifications,
                        onToggle  = { pushNotifications = it }
                    )
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        icon     = Icons.Outlined.Sms,
                        label    = "SMS Alerts",
                        subtitle = "Text messages for critical events",
                        checked  = smsAlerts,
                        onToggle = { smsAlerts = it }
                    )
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        icon     = Icons.Outlined.Email,
                        label    = "Daily Email Reports",
                        subtitle = "Activity summary sent each morning",
                        checked  = emailReports,
                        onToggle = { emailReports = it }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("System Configuration")
            TaliCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier              = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(NavySurface, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Sensors, null, tint = Navy, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    "Fall Detection Sensitivity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Navy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Higher sensitivity = more alerts",
                                    fontSize = 11.sp,
                                    color = GrayMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .background(sensitivityColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(sensitivityLabel, fontSize = 12.sp, color = sensitivityColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Low", "Medium", "High").forEach { lbl ->
                            Text(
                                text       = lbl,
                                fontSize   = 11.sp,
                                color      = if (sensitivityLabel == lbl) sensitivityColor else GrayMuted,
                                fontWeight = if (sensitivityLabel == lbl) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Slider(
                        value         = sensitivity,
                        onValueChange = { sensitivity = it },
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = SliderDefaults.colors(
                            thumbColor            = sensitivityColor,
                            activeTrackColor      = sensitivityColor,
                            inactiveTrackColor    = GrayLight
                        )
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = when (sensitivityLabel) {
                            "Low"    -> "Suitable for very active seniors. Reduces false alarms."
                            "Medium" -> "Balanced for typical daily activity. Recommended."
                            else     -> "Maximum protection. May produce occasional false alerts."
                        },
                        fontSize  = 12.sp,
                        color     = GrayMuted,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Hardware")
            TaliCard {
                Column {
                    HardwareStatusRow(
                        icon    = Icons.Filled.Videocam,
                        label   = "AI Vision Camera",
                        status  = "Paired",
                        detail  = "Living Room · 1080p · Online",
                        isPaired = true
                    )
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    HardwareStatusRow(
                        icon     = Icons.Filled.PhoneAndroid,
                        label    = "Elderly Device (IMU)",
                        status   = "Connected",
                        detail   = "Dato' Ahmad's Phone · Battery 85%",
                        isPaired = true
                    )
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    HardwareStatusRow(
                        icon     = Icons.Filled.Cloud,
                        label    = "Firebase Realtime DB",
                        status   = "Synced",
                        detail   = "Last sync 12 seconds ago",
                        isPaired = true
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Danger Zone")
            TaliCard {
                Column {
                    SettingsNavRow(
                        icon     = Icons.Outlined.ExitToApp,
                        label    = "Sign Out",
                        subtitle = "You will be logged out on this device",
                        tint     = Crimson,
                        onClick  = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon     : ImageVector,
    label    : String,
    subtitle : String,
    tint     : Color = Navy,
    onClick  : () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = tint)
            Text(subtitle, fontSize = 12.sp, color = GrayMuted)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = GrayMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    icon     : ImageVector,
    label    : String,
    subtitle : String,
    checked  : Boolean,
    onToggle : (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(NavySurface, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Navy, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Text(subtitle, fontSize = 12.sp, color = GrayMuted)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = Teal,
                uncheckedTrackColor = GrayLight
            )
        )
    }
}

@Composable
private fun HardwareStatusRow(
    icon     : ImageVector,
    label    : String,
    status   : String,
    detail   : String,
    isPaired : Boolean
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(NavySurface, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Navy, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,  fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Text(detail, fontSize = 12.sp, color = GrayMuted)
        }
        Box(
            modifier = Modifier
                .background(
                    color = if (isPaired) TealLight else CrimsonLight,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text       = status,
                fontSize   = 11.sp,
                color      = if (isPaired) Teal else Crimson,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  4. EDIT PROFILE SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun EditProfileScreen(onBackClick: () -> Unit) {
    var fullName        by remember { mutableStateOf("Siti Nurhaliza") }
    var phoneNumber     by remember { mutableStateOf("+60 12-345 6789") }
    var email           by remember { mutableStateOf("siti@talihayat.my") }
    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var isSaving        by remember { mutableStateOf(false) }

    var nameError    by remember { mutableStateOf<String?>(null) }
    var phoneError   by remember { mutableStateOf<String?>(null) }
    var passError    by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .shadow(elevation = 2.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector        = Icons.Filled.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint               = Navy,
                        modifier           = Modifier.size(20.dp)
                    )
                }

                Text(
                    text       = "Edit Profile",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Navy,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center
                )

                Spacer(Modifier.size(48.dp))
            }
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .background(TealLight, CircleShape)
                        .border(3.dp, Teal.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👩‍⚕️", fontSize = 50.sp)
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Teal, CircleShape)
                        .border(2.dp, Surface, CircleShape)
                        .clickable { /* open image picker */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Filled.CameraAlt,
                        contentDescription = "Change photo",
                        tint               = Color.White,
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text       = "Tap the camera to change photo",
                fontSize   = 12.sp,
                color      = GrayMuted,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Column(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement   = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader("Personal Information")

                ProfileTextField(
                    value         = fullName,
                    onValueChange = { fullName = it; nameError = null },
                    label         = "Full Name",
                    placeholder   = "Enter your full name",
                    icon          = Icons.Outlined.Person,
                    isError       = nameError != null,
                    errorMessage  = nameError
                )

                ProfileTextField(
                    value         = phoneNumber,
                    onValueChange = { phoneNumber = it; phoneError = null },
                    label         = "Phone Number",
                    placeholder   = "+60 12-345 6789",
                    icon          = Icons.Outlined.Phone,
                    keyboardType  = KeyboardType.Phone,
                    isError       = phoneError != null,
                    errorMessage  = phoneError
                )

                ProfileTextField(
                    value         = email,
                    onValueChange = { email = it },
                    label         = "Email Address",
                    placeholder   = "your@email.com",
                    icon          = Icons.Outlined.Email,
                    keyboardType  = KeyboardType.Email,
                    readOnly      = true,
                    trailingNote  = "Verified"
                )

                Spacer(Modifier.height(4.dp))
                SectionHeader("Change Password")
                Text(
                    text       = "Leave blank to keep your current password.",
                    fontSize   = 12.sp,
                    color      = GrayMuted,
                    modifier   = Modifier.padding(bottom = 4.dp)
                )

                ProfileTextField(
                    value         = newPassword,
                    onValueChange = { newPassword = it; passError = null },
                    label         = "New Password",
                    placeholder   = "Min. 8 characters",
                    icon          = Icons.Outlined.Lock,
                    isPassword    = true,
                    showPassword  = showPassword,
                    onTogglePass  = { showPassword = !showPassword },
                    isError       = passError != null,
                    errorMessage  = passError
                )

                ProfileTextField(
                    value         = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmError = null },
                    label         = "Confirm New Password",
                    placeholder   = "Re-enter new password",
                    icon          = Icons.Outlined.LockOpen,
                    isPassword    = true,
                    showPassword  = showPassword,
                    onTogglePass  = { showPassword = !showPassword },
                    isError       = confirmError != null,
                    errorMessage  = confirmError
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Teal, TealDark)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .shadow(
                            elevation    = 8.dp,
                            shape        = RoundedCornerShape(18.dp),
                            ambientColor = Teal.copy(alpha = 0.3f),
                            spotColor    = Teal.copy(alpha = 0.3f)
                        )
                        .clickable(enabled = !isSaving) {
                            nameError  = if (fullName.isBlank()) "Full name is required" else null
                            phoneError = if (phoneNumber.isBlank()) "Phone number is required" else null
                            passError  = if (newPassword.isNotEmpty() && newPassword.length < 8)
                                "Password must be at least 8 characters" else null
                            confirmError = if (newPassword.isNotEmpty() && confirmPassword != newPassword)
                                "Passwords do not match" else null

                            if (listOf(nameError, phoneError, passError, confirmError).all { it == null }) {
                                isSaving = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Save, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Text(
                                text          = "Save Changes",
                                fontSize      = 16.sp,
                                fontWeight    = FontWeight.ExtraBold,
                                color         = Color.White,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }

                Box(
                    modifier            = Modifier.fillMaxWidth(),
                    contentAlignment    = Alignment.Center
                ) {
                    Text(
                        text       = "Delete Account",
                        fontSize   = 13.sp,
                        color      = Crimson.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier
                            .clickable { }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value         : String,
    onValueChange : (String) -> Unit,
    label         : String,
    placeholder   : String,
    icon          : ImageVector,
    keyboardType  : KeyboardType = KeyboardType.Text,
    isPassword    : Boolean      = false,
    showPassword  : Boolean      = false,
    onTogglePass  : (() -> Unit)? = null,
    isError       : Boolean      = false,
    errorMessage  : String?      = null,
    readOnly      : Boolean      = false,
    trailingNote  : String?      = null
) {
    Column {
        OutlinedTextField(
            value             = value,
            onValueChange     = onValueChange,
            label             = { Text(label, fontSize = 13.sp) },
            placeholder       = { Text(placeholder, fontSize = 14.sp, color = GrayMuted) },
            leadingIcon       = {
                Icon(icon, null,
                    tint     = if (isError) Crimson else Teal,
                    modifier = Modifier.size(20.dp))
            },
            trailingIcon      = when {
                trailingNote != null -> ({
                    Box(
                        modifier = Modifier
                            .background(TealLight, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(trailingNote, fontSize = 10.sp, color = Teal, fontWeight = FontWeight.Bold)
                    }
                })
                isPassword -> ({
                    IconButton(onClick = { onTogglePass?.invoke() }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = GrayMuted
                        )
                    }
                })
                isError -> ({
                    Icon(Icons.Filled.ErrorOutline, null, tint = Crimson)
                })
                else -> null
            },
            visualTransformation = if (isPassword && !showPassword)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions   = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            isError           = isError,
            readOnly          = readOnly,
            modifier          = Modifier.fillMaxWidth(),
            shape             = RoundedCornerShape(14.dp),
            singleLine        = true,
            colors            = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Teal,
                unfocusedBorderColor    = GrayLight,
                errorBorderColor        = Crimson,
                errorLeadingIconColor   = Crimson,
                disabledBorderColor     = GrayLight,
                cursorColor             = Teal
            )
        )

        AnimatedVisibility(
            visible = isError && !errorMessage.isNullOrBlank(),
            enter   = expandVertically(tween(200)) + fadeIn(tween(150)),
            exit    = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            Row(
                modifier              = Modifier.padding(top = 4.dp, start = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.ErrorOutline, null,
                    tint = Crimson, modifier = Modifier.size(13.dp))
                Text(errorMessage.orEmpty(), fontSize = 11.sp, color = Crimson)
            }
        }
    }
}

@Composable
fun TaliCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Navy.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
    }
}