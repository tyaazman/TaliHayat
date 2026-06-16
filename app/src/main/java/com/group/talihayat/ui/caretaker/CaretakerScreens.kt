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
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.group.talihayat.ui.theme.*

// ─────────────────────────────────────────────────
//  SHARED CARD COMPONENT
// ─────────────────────────────────────────────────

@Composable
private fun TaliCard(
    modifier  : Modifier = Modifier,
    content   : @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.shadow(
            elevation    = 6.dp,
            shape        = RoundedCornerShape(20.dp),
            ambientColor = TaliColors.CardShadow,
            spotColor    = TaliColors.CardShadow
        ),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = TaliColors.Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
    }
}

// ─────────────────────────────────────────────────
//  SHARED SECTION HEADER
// ─────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text          = title.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        color         = TaliColors.GrayMuted,
        letterSpacing = 1.5.sp,
        modifier      = modifier.padding(bottom = 10.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  1. REPORTS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ReportsScreen() {

    // ── Dummy incident data ──────────────────────────────────────────────────
    data class Incident(
        val time        : String,
        val description : String,
        val severity    : String,   // "critical" | "warning" | "info"
        val date        : String
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

    // ── Weekly bar chart data (0f–1f relative height) ───────────────────────
    val days    = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    val steps   = listOf(0.45f, 0.72f, 0.38f, 0.88f, 0.60f, 0.30f, 0.55f)
    val falls   = listOf(0,     0,     1,     0,     0,     0,     1    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TaliColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Page title ───────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text       = "Reports",
                fontSize   = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = TaliColors.Navy
            )
            Text(
                text     = "Week of June 2–8, 2025",
                fontSize = 13.sp,
                color    = TaliColors.GrayMuted
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Summary tiles ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryTile(
                label  = "Falls",
                value  = "2",
                icon   = Icons.Filled.Warning,
                color  = TaliColors.CrimsonAlert,
                bgColor = TaliColors.CrimsonLight,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label  = "Alerts",
                value  = "5",
                icon   = Icons.Filled.Notifications,
                color  = TaliColors.AmberWarning,
                bgColor = Color(0xFFFFF8E1),
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label  = "Safe Days",
                value  = "5",
                icon   = Icons.Filled.HealthAndSafety,
                color  = TaliColors.TealSafe,
                bgColor = TaliColors.TealLight,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Weekly Activity Chart ────────────────────────────────────────────
        TaliCard(modifier = Modifier.padding(horizontal = 24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text       = "Weekly Activity",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TaliColors.Navy
                        )
                        Text(
                            text     = "Steps & fall events",
                            fontSize = 12.sp,
                            color    = TaliColors.GrayMuted
                        )
                    }
                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendDot(color = TaliColors.TealSafe, label = "Activity")
                        LegendDot(color = TaliColors.CrimsonAlert, label = "Fall")
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Bar chart canvas
                WeeklyBarChart(
                    days   = days,
                    steps  = steps,
                    falls  = falls,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Incident History ─────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader(title = "Incident History")

            TaliCard {
                Column {
                    incidents.forEachIndexed { index, incident ->
                        IncidentRow(incident = incident)
                        if (index < incidents.lastIndex) {
                            HorizontalDivider(
                                color     = TaliColors.Divider,
                                thickness = 0.5.dp,
                                modifier  = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Summary tile ──────────────────────────────────────────────────────────────

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
            modifier            = Modifier.padding(14.dp),
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
                color      = TaliColors.Navy
            )
            Text(
                text     = label,
                fontSize = 11.sp,
                color    = TaliColors.GrayMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Legend dot ────────────────────────────────────────────────────────────────

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
        Text(text = label, fontSize = 10.sp, color = TaliColors.GrayMuted)
    }
}

// ── Weekly bar chart (Canvas) ─────────────────────────────────────────────────

@Composable
private fun WeeklyBarChart(
    days     : List<String>,
    steps    : List<Float>,
    falls    : List<Int>,
    modifier : Modifier = Modifier
) {
    // Animate bars growing up from zero on first composition
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

                // Shadow
                drawRoundRect(
                    color     = Color.Black.copy(alpha = 0.05f),
                    topLeft   = Offset(x + 2f, barTop + 4f),
                    size      = Size(barWidth, barH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                // Bar fill — gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors     = if (hasFall)
                            listOf(
                                TaliColors.CrimsonAlert,
                                TaliColors.CrimsonAlert.copy(alpha = 0.65f)
                            )
                        else listOf(
                            TaliColors.TealSafe,
                            TaliColors.TealSafe.copy(alpha = 0.55f)
                        ),
                        startY = barTop,
                        endY   = size.height
                    ),
                    topLeft      = Offset(x, barTop),
                    size         = Size(barWidth, barH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                // Fall indicator dot on top of bar
                if (hasFall) {
                    drawCircle(
                        color  = TaliColors.CrimsonAlert,
                        radius = 5.dp.toPx(),
                        center = Offset(x + barWidth / 2f, barTop - 8.dp.toPx())
                    )
                }
            }
        }

        // Day labels row
        Spacer(Modifier.height(6.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEachIndexed { i, day ->
                Text(
                    text      = day,
                    fontSize  = 11.sp,
                    color     = if (falls[i] > 0) TaliColors.CrimsonAlert else TaliColors.GrayMuted,
                    fontWeight = if (falls[i] > 0) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Incident row ──────────────────────────────────────────────────────────────

@Composable
private fun IncidentRow(incident: Any) {
    // Using reflection-free data via destructuring with a local data class
    data class Inc(
        val time: String, val description: String,
        val severity: String, val date: String
    )

    @Suppress("UNCHECKED_CAST")
    val inc = incident as? Inc

    // Because we can't cast to private data class — inline the 7 rows manually:
    // This composable accepts a raw triple instead
}

// Inline incident list item (replaces IncidentRow since data class is private scope)
@Composable
private fun IncidentItem(
    time        : String,
    description : String,
    severity    : String,   // "critical" | "warning" | "info"
    date        : String
) {
    val (iconVec, iconColor, bgColor) = when (severity) {
        "critical" -> Triple(Icons.Filled.Warning,           TaliColors.CrimsonAlert, TaliColors.CrimsonLight)
        "warning"  -> Triple(Icons.Filled.NotificationImportant, TaliColors.AmberWarning,  Color(0xFFFFF8E1))
        else       -> Triple(Icons.Filled.CheckCircle,        TaliColors.TealSafe,    TaliColors.TealLight)
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
                color      = TaliColors.Navy
            )
            Text(
                text     = "$date · $time",
                fontSize = 12.sp,
                color    = TaliColors.GrayMuted
            )
        }

        // Severity badge
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

// Override ReportsScreen to use the correct incident item composable ───────────

// NOTE: Replace the placeholder IncidentRow calls in the TaliCard above
// with the properly-typed list below. The ReportsScreen is re-declared
// as a clean, fully working version:

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreenFull() {

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

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

    // 👇 1. PLACE THE PULL TO REFRESH BOX OPENER HERE 👇
    // ✅ Change lines 521-524 to look exactly like this:
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                delay(1500)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize() // 🟢 Just the modifier here!
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TaliColors.Background)
                .statusBarsPadding() // 🟢 Add this line to handle the phone status bar automatically
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("Reports", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TaliColors.Navy)
                Text("Week of June 2–8, 2025", fontSize = 13.sp, color = TaliColors.GrayMuted)
            }

            Spacer(Modifier.height(24.dp))

            // Summary tiles
            Row(
                modifier              = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryTile("Falls",     "2", Icons.Filled.Warning,         TaliColors.CrimsonAlert, TaliColors.CrimsonLight, Modifier.weight(1f))
                SummaryTile("Alerts",    "5", Icons.Filled.Notifications,   TaliColors.AmberWarning, Color(0xFFFFF8E1),       Modifier.weight(1f))
                SummaryTile("Safe Days", "5", Icons.Filled.HealthAndSafety, TaliColors.TealSafe,     TaliColors.TealLight,    Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Chart card
            TaliCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Weekly Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TaliColors.Navy)
                            Text("Steps & fall events", fontSize = 12.sp, color = TaliColors.GrayMuted)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendDot(TaliColors.TealSafe,    "Activity")
                            LegendDot(TaliColors.CrimsonAlert,"Fall")
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    WeeklyBarChart(days = days, steps = steps, falls = falls, modifier = Modifier.fillMaxWidth().height(140.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Incident history
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
                                    color     = TaliColors.Divider,
                                    thickness = 0.5.dp,
                                    modifier  = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } // 👈 2. ADD THIS CLOSING BRACE AT THE VERY END OF THE FUNCTION
}

// ═══════════════════════════════════════════════════════════════════════════════
//  2. PATIENT SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class) // 👈 ADD THIS OPT-IN LINE ABOVE THE FUNCTION
@Composable
fun PatientScreen() {
    // 👇 1. INITIALIZE THE REFRESH STATES AT THE TOP 👇
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // 👇 2. PLACE THE PULL TO REFRESH BOX OPENER HERE 👇
    // ✅ Update the container to only use the modifier:
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                delay(1500)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TaliColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // ── Gradient hero header ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(TaliColors.Navy, TaliColors.Navy.copy(alpha = 0.85f))
                        )
                    )
                    .padding(top = 48.dp, bottom = 32.dp)
            ) {
                // Subtle ambient blob
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color  = TaliColors.TealSafe.copy(alpha = 0.12f),
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
                    // Avatar
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(TaliColors.TealLight, CircleShape)
                                .border(3.dp, TaliColors.TealSafe.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👴", fontSize = 40.sp)
                        }
                        // Status dot
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(TaliColors.GreenOnline, CircleShape)
                                .border(2.dp, TaliColors.Navy, CircleShape)
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

                    // Status tags
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PatientTag(label = "🛡 Sensor Armed", color = TaliColors.TealSafe)
                        PatientTag(label = "🔋 Battery 85%",  color = TaliColors.GreenOnline)
                        PatientTag(label = "📡 Online",        color = Color(0xFF5B6EF5))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Live metrics row ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile("72",        "BPM",         "Heart Rate", TaliColors.CrimsonAlert, Modifier.weight(1f))
                MetricTile("98.2°F",    "",             "Temperature", TaliColors.AmberWarning, Modifier.weight(1f))
                MetricTile("0",         "Falls",        "Today",      TaliColors.TealSafe,     Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Emergency Contacts ───────────────────────────────────────────────
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
                                HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Medical Profile ──────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SectionHeader("Medical Profile")
                TaliCard {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MedicalRow(Icons.Filled.Bloodtype,   "Blood Type",              "B+")
                        HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)
                        MedicalRow(Icons.Filled.Warning,     "Allergies",               "Penicillin, Shellfish")
                        HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)
                        MedicalRow(Icons.Filled.LocalHospital,"Underlying Conditions",  "Hypertension, Type 2 Diabetes")
                        HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)
                        MedicalRow(Icons.Filled.Medication,  "Current Medications",     "Metformin 500mg, Amlodipine 5mg")
                        HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)
                        MedicalRow(Icons.Filled.CalendarMonth,"Last Check-up",          "15 May 2025")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Location card ────────────────────────────────────────────────────
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
                                .background(TaliColors.NavyLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LocationOn, null, tint = TaliColors.Navy, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Living Room", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TaliColors.Navy)
                            Text("Last detected 3 minutes ago", fontSize = 12.sp, color = TaliColors.GrayMuted)
                        }
                        Box(
                            modifier = Modifier
                                .background(TaliColors.TealLight, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("LIVE", fontSize = 10.sp, color = TaliColors.TealSafe, fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    } // 👈 3. ADD THIS CLOSING BRACE AT THE VERY END OF THE FUNCTION
}

// ── Patient tag pill ──────────────────────────────────────────────────────────

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

// ── Metric tile ───────────────────────────────────────────────────────────────

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
            modifier            = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
                if (unit.isNotEmpty()) {
                    Text(unit, fontSize = 11.sp, color = color, modifier = Modifier.padding(bottom = 3.dp, start = 2.dp))
                }
            }
            Text(label, fontSize = 11.sp, color = TaliColors.GrayMuted, textAlign = TextAlign.Center)
        }
    }
}

// ── Contact row ───────────────────────────────────────────────────────────────

@Composable
private fun ContactRow(name: String, role: String, phone: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar initials
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(TaliColors.NavyLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = name.first().toString(),
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = TaliColors.Navy
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TaliColors.Navy)
            Text(role, fontSize = 12.sp, color = TaliColors.GrayMuted)
        }

        // Call button
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(TaliColors.TealLight, CircleShape)
                .clickable { /* trigger phone intent */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Call, "Call $name", tint = TaliColors.TealSafe, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Medical info row ──────────────────────────────────────────────────────────

@Composable
private fun MedicalRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TaliColors.GrayMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = TaliColors.GrayMuted, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, color = TaliColors.Navy, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  3. SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToPrivacySecurity: () -> Unit
) {
    val context = LocalContext.current
    // ── Local state ──────────────────────────────────────────────────────────
    var pushNotifications by remember { mutableStateOf(true) }
    var smsAlerts         by remember { mutableStateOf(true) }
    var emailReports      by remember { mutableStateOf(false) }
    var sensitivity       by remember { mutableStateOf(0.65f) }  // 0f=Low, 0.5f=Med, 1f=High

    val sensitivityLabel = when {
        sensitivity < 0.33f -> "Low"
        sensitivity < 0.67f -> "Medium"
        else                -> "High"
    }
    val sensitivityColor = when (sensitivityLabel) {
        "Low"    -> TaliColors.GreenOnline
        "Medium" -> TaliColors.AmberWarning
        else     -> TaliColors.CrimsonAlert
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TaliColors.Background)
            .statusBarsPadding() // 🟢 Add this line here as well
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TaliColors.Navy)
            Text("Manage your TaliHayat configuration", fontSize = 13.sp, color = TaliColors.GrayMuted)
        }

        Spacer(Modifier.height(28.dp))

        // ── ACCOUNT ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Account")
            TaliCard {
                Column {
                    // Profile row with current user info
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(TaliColors.NavyLight, CircleShape)
                                .border(2.dp, TaliColors.TealSafe.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👩‍⚕️", fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Siti Nurhaliza", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TaliColors.Navy)
                            Text("Primary Caregiver", fontSize = 12.sp, color = TaliColors.GrayMuted)
                        }
                    }

                    HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)

                    // Edit Profile row
                    SettingsNavRow(
                        icon      = Icons.Outlined.Edit,
                        label     = "Edit Profile",
                        subtitle  = "Update your name, photo & password",
                        onClick   = onNavigateToEditProfile
                    )

                    HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)

                    SettingsNavRow(
                        icon     = Icons.Outlined.Security,
                        label    = "Privacy & Security",
                        subtitle = "Two-factor auth, data sharing",
                        onClick  = onNavigateToPrivacySecurity
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── PREFERENCES ───────────────────────────────────────────────────────
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
                    HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)
                    SettingsToggleRow(
                        icon     = Icons.Outlined.Sms,
                        label    = "SMS Alerts",
                        subtitle = "Text messages for critical events",
                        checked  = smsAlerts,
                        onToggle = { smsAlerts = it }
                    )
                    HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)
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

        // ── HARDWARE ─────────────────────────────────────────────────────────
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
                    HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)
                    HardwareStatusRow(
                        icon     = Icons.Filled.PhoneAndroid,
                        label    = "Elderly Device (IMU)",
                        status   = "Connected",
                        detail   = "Dato' Ahmad's Phone · Battery 85%",
                        isPaired = true
                    )
                    HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)
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

        // ── DANGER ZONE ───────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("Danger Zone")
            TaliCard {
                Column {
                    SettingsNavRow(
                        icon     = Icons.Outlined.ExitToApp, // 👈 Switch to standard Outlined here
                        label    = "Sign Out",
                        subtitle = "You will be logged out on this device",
                        tint     = TaliColors.CrimsonAlert,
                        onClick  = {
                            // ➔ 🟢 REPLACE THE EMPTY CLOSURE WITH THIS NAVIGATION LOGIC:

                            // Note: If you use Firebase Auth later, uncomment the line below:
                            // com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

                            val intent = Intent(context, com.group.talihayat.ui.auth.AuthActivity::class.java).apply {
                                // These flags clear the history stack so pressing "Back" won't re-open the dashboard
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

// ── Settings nav row ──────────────────────────────────────────────────────────

@Composable
private fun SettingsNavRow(
    icon     : ImageVector,
    label    : String,
    subtitle : String,
    tint     : Color = TaliColors.Navy,
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
            Text(subtitle, fontSize = 12.sp, color = TaliColors.GrayMuted)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TaliColors.GrayMuted, modifier = Modifier.size(18.dp))
    }
}

// ── Settings toggle row ───────────────────────────────────────────────────────

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
                .background(TaliColors.NavyLight, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TaliColors.Navy, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TaliColors.Navy)
            Text(subtitle, fontSize = 12.sp, color = TaliColors.GrayMuted)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = TaliColors.TealSafe,
                uncheckedTrackColor = TaliColors.GrayLight
            )
        )
    }
}

// ── Hardware status row ───────────────────────────────────────────────────────

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
                .background(TaliColors.NavyLight, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TaliColors.Navy, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,  fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TaliColors.Navy)
            Text(detail, fontSize = 12.sp, color = TaliColors.GrayMuted)
        }
        Box(
            modifier = Modifier
                .background(
                    color = if (isPaired) TaliColors.TealLight else TaliColors.CrimsonLight,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text       = status,
                fontSize   = 11.sp,
                color      = if (isPaired) TaliColors.TealSafe else TaliColors.CrimsonAlert,
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

    // ── Field state ──────────────────────────────────────────────────────────
    var fullName        by remember { mutableStateOf("Siti Nurhaliza") }
    var phoneNumber     by remember { mutableStateOf("+60 12-345 6789") }
    var email           by remember { mutableStateOf("siti@talihayat.my") }
    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var isSaving        by remember { mutableStateOf(false) }

    // ── Validation errors ────────────────────────────────────────────────────
    var nameError    by remember { mutableStateOf<String?>(null) }
    var phoneError   by remember { mutableStateOf<String?>(null) }
    var passError    by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TaliColors.Background)
    ) {
        // ── Top App Bar ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TaliColors.Surface)
                .shadow(elevation = 2.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector        = Icons.Filled.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint               = TaliColors.Navy,
                        modifier           = Modifier.size(20.dp)
                    )
                }

                Text(
                    text       = "Edit Profile",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TaliColors.Navy,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center
                )

                // Spacer to balance the back button
                Spacer(Modifier.size(48.dp))
            }
        }

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Profile picture placeholder ───────────────────────────────────
            Box(contentAlignment = Alignment.BottomEnd) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .background(TaliColors.TealLight, CircleShape)
                        .border(3.dp, TaliColors.TealSafe.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👩‍⚕️", fontSize = 50.sp)
                }

                // Edit badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(TaliColors.TealSafe, CircleShape)
                        .border(2.dp, TaliColors.Surface, CircleShape)
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
                color      = TaliColors.GrayMuted,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // ── Form fields ───────────────────────────────────────────────────
            Column(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement   = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader("Personal Information")

                // Full Name
                ProfileTextField(
                    value         = fullName,
                    onValueChange = { fullName = it; nameError = null },
                    label         = "Full Name",
                    placeholder   = "Enter your full name",
                    icon          = Icons.Outlined.Person,
                    isError       = nameError != null,
                    errorMessage  = nameError
                )

                // Phone
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

                // Email (read-only display)
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
                    color      = TaliColors.GrayMuted,
                    modifier   = Modifier.padding(bottom = 4.dp)
                )

                // New Password
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

                // Confirm Password
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

                // ── Save Changes Button ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(TaliColors.TealSafe, TaliColors.TealDark)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .shadow(
                            elevation    = 8.dp,
                            shape        = RoundedCornerShape(18.dp),
                            ambientColor = TaliColors.TealSafe.copy(alpha = 0.3f),
                            spotColor    = TaliColors.TealSafe.copy(alpha = 0.3f)
                        )
                        .clickable(enabled = !isSaving) {
                            // Validate
                            nameError  = if (fullName.isBlank()) "Full name is required" else null
                            phoneError = if (phoneNumber.isBlank()) "Phone number is required" else null
                            passError  = if (newPassword.isNotEmpty() && newPassword.length < 8)
                                "Password must be at least 8 characters" else null
                            confirmError = if (newPassword.isNotEmpty() && confirmPassword != newPassword)
                                "Passwords do not match" else null

                            if (listOf(nameError, phoneError, passError, confirmError).all { it == null }) {
                                isSaving = true
                                // Simulate save — replace with real ViewModel call
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

                // Delete account — subtle danger link
                Box(
                    modifier            = Modifier.fillMaxWidth(),
                    contentAlignment    = Alignment.Center
                ) {
                    Text(
                        text       = "Delete Account",
                        fontSize   = 13.sp,
                        color      = TaliColors.CrimsonAlert.copy(alpha = 0.7f),
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

// ── Premium profile OutlinedTextField ─────────────────────────────────────────

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
            placeholder       = { Text(placeholder, fontSize = 14.sp, color = TaliColors.GrayMuted) },
            leadingIcon       = {
                Icon(icon, null,
                    tint     = if (isError) TaliColors.CrimsonAlert else TaliColors.TealSafe,
                    modifier = Modifier.size(20.dp))
            },
            trailingIcon      = when {
                trailingNote != null -> ({
                    Box(
                        modifier = Modifier
                            .background(TaliColors.TealLight, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(trailingNote, fontSize = 10.sp, color = TaliColors.TealSafe, fontWeight = FontWeight.Bold)
                    }
                })
                isPassword -> ({
                    IconButton(onClick = { onTogglePass?.invoke() }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = TaliColors.GrayMuted
                        )
                    }
                })
                isError -> ({
                    Icon(Icons.Filled.ErrorOutline, null, tint = TaliColors.CrimsonAlert)
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
                focusedBorderColor      = TaliColors.TealSafe,
                unfocusedBorderColor    = TaliColors.GrayLight,
                errorBorderColor        = TaliColors.CrimsonAlert,
                errorLeadingIconColor   = TaliColors.CrimsonAlert,
                disabledBorderColor     = TaliColors.GrayLight,
                cursorColor             = TaliColors.TealSafe
            )
        )

        // Inline error message
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
                    tint = TaliColors.CrimsonAlert, modifier = Modifier.size(13.dp))
                Text(errorMessage.orEmpty(), fontSize = 11.sp, color = TaliColors.CrimsonAlert)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  USAGE GUIDE — Wire these into your bottom navigation
//
//  In your CaretakerDashboardActivity (or NavHost):
//
//  val tabs = listOf("Home","Reports","Patient","Settings")
//  var selectedTab by remember { mutableStateOf(0) }
//
//  when (selectedTab) {
//      0 -> TaliHayatDashboard()           // your existing home screen
//      1 -> ReportsScreenFull()            // use ReportsScreenFull (not ReportsScreen)
//      2 -> PatientScreen()
//      3 -> SettingsScreen(
//               onNavigateToEditProfile = { showEditProfile = true }
//           )
//  }
//
//  if (showEditProfile) {
//      EditProfileScreen(onBackClick = { showEditProfile = false })
//  }
// ═══════════════════════════════════════════════════════════════════════════════


// ═══════════════════════════════════════════════════════════════════════════════
//  CARETAKER HOME SCREEN  —  Main Dashboard
//  Replaces the previous donut-chart layout with a warm, family-friendly UI.
//  Public entry point: CaretakerHomeScreen()
// ═══════════════════════════════════════════════════════════════════════════════

// ── Extra tokens used only in this screen ────────────────────────────────────
// (all others come from TaliColors above)
private val HomeGreenLight  = Color(0xFFEDF7ED)
private val HomeGreen       = Color(0xFF2E7D32)
private val HomeLiveBadge   = Color(0xFF00C853)
private val HomeHeartRed    = Color(0xFFE53935)
private val HomeAmber       = Color(0xFFFFA000)
private val HomeAmberLight  = Color(0xFFFFF8E1)
private val HomeNavyGrad    = Color(0xFF2D5288)

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaretakerHomeScreen(elderlySteps: Int, elderlyBattery: Int) {

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // ── Notification badge & dialog states ───────────────────────────────────
    val hasUnseenAlerts = remember { mutableStateOf(true) }
    var showNotificationDialog by remember { mutableStateOf(false) } // 🟢 Tracks the modal state

    // ── Breathing animation for the safe-status green ring ───────────────────
    val breatheInfinite = rememberInfiniteTransition(label = "SafeBreath")
    val breatheAlpha by breatheInfinite.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0.70f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "BreathAlpha"
    )

    // ── Pulsing live-dot for location card ────────────────────────────────────
    val livePulse = rememberInfiniteTransition(label = "LiveDot")
    val liveDotScale by livePulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.35f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "LiveDotScale"
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                delay(1500)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TaliColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {

            // ══════════════════════════════════════════════════════════════════════
            //  1. HEADER
            // ══════════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TaliColors.Surface)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color  = TaliColors.TealSafe.copy(alpha = 0.06f),
                        radius = size.width * 0.5f,
                        center = Offset(size.width * 1.1f, -size.height * 0.3f)
                    )
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text          = "TaliHayat",
                            fontSize      = 26.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = TaliColors.Navy,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text      = "Caretaker Portal",
                            fontSize  = 13.sp,
                            color     = TaliColors.GrayMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(4.dp, CircleShape,
                                    ambientColor = TaliColors.CardShadow,
                                    spotColor    = TaliColors.CardShadow)
                                .background(TaliColors.Background, CircleShape)
                                .clickable {
                                    hasUnseenAlerts.value = false
                                    showNotificationDialog = true // 🟢 Opens the notification system overlay
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint               = TaliColors.Navy,
                                modifier           = Modifier.size(24.dp)
                            )
                        }

                        this@Row.AnimatedVisibility(
                            visible = hasUnseenAlerts.value,
                            enter   = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                            exit    = scaleOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = (-2).dp, y = 2.dp)
                                    .background(TaliColors.CrimsonAlert, CircleShape)
                                    .border(1.5.dp, TaliColors.Surface, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════════════════
            //  2. REASSURING STATUS CARD
            // ══════════════════════════════════════════════════════════════════════
            TaliCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color  = HomeGreen.copy(alpha = breatheAlpha * 0.08f),
                            radius = size.width * 0.55f,
                            center = Offset(size.width * 0.15f, size.height / 2f)
                        )
                    }

                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier.size(70.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                HomeLiveBadge.copy(alpha = breatheAlpha * 0.35f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(HomeGreenLight, CircleShape)
                                    .border(2.dp, HomeLiveBadge.copy(alpha = 0.45f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Filled.HealthAndSafety,
                                    contentDescription = null,
                                    tint               = HomeGreen,
                                    modifier           = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = "Dato' Ahmad Razali is Safe",
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = TaliColors.Navy,
                                lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text       = "All sensors working perfectly  •  Checked just now",
                                fontSize   = 12.sp,
                                color      = TaliColors.GrayMuted,
                                lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .background(HomeGreenLight, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(HomeLiveBadge, CircleShape)
                                    )
                                    Text(
                                        text       = "All Clear",
                                        fontSize   = 11.sp,
                                        color      = HomeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════════════════════════════════════
            //  3. HEALTH & PHONE STATS GRID
            // ══════════════════════════════════════════════════════════════════════
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TaliCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(TaliColors.CrimsonLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint               = HomeHeartRed,
                                modifier           = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                    }
                }

                TaliCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(TaliColors.NavyLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint               = TaliColors.Navy,
                                modifier           = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text       = "$elderlyBattery%", // 🟢 Displays live cloud battery level!
                                fontSize   = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = when {
                                elderlyBattery <= 20 -> TaliColors.CrimsonAlert // Turn red if critically low!
                                else -> TaliColors.Navy
                            }
                            )
                        }

                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.SignalCellularAlt,
                                contentDescription = null,
                                tint               = HomeLiveBadge,
                                modifier           = Modifier.size(14.dp)
                            )
                            Text(
                                text     = "Strong Signal",
                                fontSize = 12.sp,
                                color    = TaliColors.GrayMuted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════════════════════════════════════
            //  4. CURRENT LOCATION CARD
            // ══════════════════════════════════════════════════════════════════════
            TaliCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(TaliColors.TealLight, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint               = TaliColors.TealSafe,
                            modifier           = Modifier.size(26.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Currently in: Living Room",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = TaliColors.Navy
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text     = "Detected 3 minutes ago",
                            fontSize = 12.sp,
                            color    = TaliColors.GrayMuted
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .background(HomeGreenLight, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp * liveDotScale)
                                    .background(HomeLiveBadge, CircleShape)
                            )
                            Text(
                                text          = "LIVE",
                                fontSize      = 10.sp,
                                fontWeight    = FontWeight.ExtraBold,
                                color         = HomeGreen,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════════════════
            //  5. QUICK ACTIONS
            // ══════════════════════════════════════════════════════════════════════
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text          = "QUICK ACTIONS",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = TaliColors.GrayMuted,
                    letterSpacing = 1.5.sp,
                    modifier      = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        icon       = Icons.Filled.Call,
                        label      = "Call Dato' Ahmad",
                        background = Brush.horizontalGradient(
                            listOf(TaliColors.TealSafe, TaliColors.TealDark)
                        ),
                        shadowColor = TaliColors.TealSafe,
                        onClick    = { },
                        modifier   = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        icon       = Icons.Filled.Videocam,
                        label      = "Check Camera\nRoom",
                        background = Brush.horizontalGradient(
                            listOf(TaliColors.Navy, HomeNavyGrad)
                        ),
                        shadowColor = TaliColors.Navy,
                        onClick    = { },
                        modifier   = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════════════════
            //  6. TODAY'S SUMMARY
            // ══════════════════════════════════════════════════════════════════════
            TaliCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text(
                        text          = "TODAY'S SUMMARY",
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = TaliColors.GrayMuted,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HomeSummaryStat(
                            value     = "%,d".format(elderlySteps), // 🟢 Injects your live synced steps!
                            label     = "Steps\nToday",
                            valueColor = TaliColors.Navy
                        )
                        HomeStatDivider()
                        HomeSummaryStat(
                            value     = "8h 14m",
                            label     = "Active\nToday",
                            valueColor = TaliColors.TealSafe
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════════════════════════════════════
            //  7. LATEST ACTIVITY
            // ══════════════════════════════════════════════════════════════════════
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text          = "LATEST ACTIVITY",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = TaliColors.GrayMuted,
                    letterSpacing = 1.5.sp,
                    modifier      = Modifier.padding(bottom = 12.dp)
                )

                TaliCard {
                    Column {
                        HomeActivityRow(
                            icon       = Icons.Filled.HealthAndSafety,
                            iconColor  = HomeLiveBadge,
                            iconBg     = HomeGreenLight,
                            title      = "Daily check-in passed",
                            subtitle   = "Today, 8:00 AM"
                        )
                        HorizontalDivider(
                            color    = TaliColors.Divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HomeActivityRow(
                            icon       = Icons.Filled.LocationOn,
                            iconColor  = TaliColors.TealSafe,
                            iconBg     = TaliColors.TealLight,
                            title      = "Moved to: Kitchen",
                            subtitle   = "Today, 7:42 AM"
                        )
                        HorizontalDivider(
                            color    = TaliColors.Divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HomeActivityRow(
                            icon       = Icons.Filled.Bedtime,
                            iconColor  = TaliColors.Navy,
                            iconBg     = TaliColors.NavyLight,
                            title      = "Rest period ended",
                            subtitle   = "Today, 6:58 AM"
                        )
                    }
                }
            }
        }

        // ── 🟢 NEW: CARETAKERS SYSTEM NOTIFICATION OVERLAY DIALOG ─────────────
        if (showNotificationDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationDialog = false },
                containerColor   = TaliColors.Surface,
                shape            = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = TaliColors.Navy,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Recent Logs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TaliColors.Navy
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        CaretakerNotificationRow(
                            icon = Icons.Filled.Warning,
                            iconColor = TaliColors.CrimsonAlert,
                            iconBg = TaliColors.CrimsonLight,
                            title = "Critical Fall Warning",
                            time = "2:14 PM",
                            description = "Dato' Ahmad Razali — Impact registered in Living Room Node."
                        )
                        HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)

                        CaretakerNotificationRow(
                            icon = Icons.Filled.BatteryAlert,
                            iconColor = TaliColors.AmberWarning,
                            iconBg = Color(0xFFFFF8E1),
                            title = "Low Battery Log",
                            time = "9:05 AM",
                            description = "Elderly primary phone tracking device dropped below 18%."
                        )
                        HorizontalDivider(color = TaliColors.Divider, thickness = 0.5.dp)

                        CaretakerNotificationRow(
                            icon = Icons.Filled.CheckCircle,
                            iconColor = TaliColors.TealSafe,
                            iconBg = TaliColors.TealLight,
                            title = "All Systems Online",
                            time = "8:00 AM",
                            description = "Hardware handshake completed. All sensor fields running safely."
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showNotificationDialog = false },
                        shape   = RoundedCornerShape(14.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = TaliColors.Navy)
                    ) {
                        Text(text = "Close", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUB-COMPOSABLES  —  used only by CaretakerHomeScreen
// ─────────────────────────────────────────────────────────────────────────────

    /** Large gradient action button used in the Quick Actions row. */
    @Composable
    private fun QuickActionButton(
        icon: ImageVector,
        label: String,
        background: Brush,
        shadowColor: Color,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .height(80.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = shadowColor.copy(alpha = 0.22f),
                    spotColor = shadowColor.copy(alpha = 0.22f)
                )
                .background(brush = background, shape = RoundedCornerShape(18.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }

    /** Single stat column used inside the Today's Summary strip. */
    @Composable
    private fun HomeSummaryStat(
        value: String,
        label: String,
        valueColor: Color
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TaliColors.GrayMuted,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }

    /** Thin vertical divider between summary stats. */
    @Composable
    private fun HomeStatDivider() {
        Box(
            modifier = Modifier
                .height(36.dp)
                .width(0.8.dp)
                .background(TaliColors.GrayLight)
        )
    }

    /** Single row in the Latest Activity feed. */
    @Composable
    private fun HomeActivityRow(
        icon: ImageVector,
        iconColor: Color,
        iconBg: Color,
        title: String,
        subtitle: String
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TaliColors.Navy
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TaliColors.GrayMuted
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TaliColors.GrayLight,
                modifier = Modifier.size(18.dp)
            )
        }
    }

@Composable
private fun CaretakerNotificationRow(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    time: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TaliColors.Navy)
                Text(text = time, fontSize = 11.sp, color = TaliColors.GrayMuted)
            }
            Spacer(Modifier.height(2.dp))
            Text(text = description, fontSize = 12.sp, color = TaliColors.GrayMuted, lineHeight = 16.sp)
        }
    }
}