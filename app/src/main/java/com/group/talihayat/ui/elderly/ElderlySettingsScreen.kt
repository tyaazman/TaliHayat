package com.group.talihayat.ui.elderly

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.group.talihayat.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────
data class ProfileFormState(
    val name:            String = "",
    val phone:           String = "",
    val age:             String = "",
    val gender:          Gender = Gender.Unset,
    val password:        String = "",
    val retypePassword:  String = "",
)

enum class Gender { Male, Female, Unset }

// ─────────────────────────────────────────────────────────────────────────────
//  MAIN SETTINGS SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderlySettingsScreen(
    fontSize: AppFontSize,
    highContrast: Boolean,
    countdownDuration: Int,
    fallSensitivity: Float,
    onFontSizeChange: (AppFontSize) -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    onCountdownDurationChange: (Int) -> Unit,
    onFallSensitivityChange: (Float) -> Unit,
    onLogout: () -> Unit
) {
    // ── FIREBASE INITIALIZATION ──
    val database = FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    // ── PROFILE STATE ──
    var profileName by remember { mutableStateOf("") }
    var profilePhone by remember { mutableStateOf("") }
    var profileAge by remember { mutableStateOf("") }
    var profileGender by remember { mutableStateOf(Gender.Unset) }

    // 🟢 Read Live Profile Data from Firebase
    LaunchedEffect(currentUid) {
        if (currentUid != null) {
            database.child("users").child(currentUid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        profileName = snapshot.child("name").getValue(String::class.java) ?: ""
                        profilePhone = snapshot.child("phone").getValue(String::class.java) ?: ""
                        profileAge = snapshot.child("age").getValue(String::class.java) ?: ""
                        val genderStr = snapshot.child("gender").getValue(String::class.java) ?: "Unset"
                        profileGender = try { Gender.valueOf(genderStr) } catch(e: Exception) { Gender.Unset }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    var currentSubScreen by remember { mutableStateOf("Main") }
    val volumeIsLoud       by remember { mutableStateOf(true) }
    val gpsEnabled         by remember { mutableStateOf(true) }
    val batteryOk          by remember { mutableStateOf(true) }
    var showLogoutDialog   by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            val enterTransition = fadeIn(animationSpec = tween(durationMillis = 280, delayMillis = 40, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.93f, animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing))
            val exitTransition = fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.97f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            enterTransition togetherWith exitTransition
        },
        label = "subscreen_spatial_navigation"
    ) { targetScreen ->
        when (targetScreen) {
            "Profile" -> {
                // 🟢 Inject Firebase data into the Edit Form!
                EditProfileScreen(
                    initialState = ProfileFormState(
                        name = profileName,
                        phone = profilePhone,
                        age = profileAge,
                        gender = profileGender,
                        password = "",
                        retypePassword = ""
                    ),
                    onSaveClick = { state ->
                        // 🟢 Save straight back to Firebase!
                        currentUid?.let { uid ->
                            val userRef = database.child("users").child(uid)
                            userRef.child("name").setValue(state.name)
                            userRef.child("phone").setValue(state.phone)
                            userRef.child("age").setValue(state.age)
                            userRef.child("gender").setValue(state.gender.name)
                            if (state.password.isNotBlank()) {
                                userRef.child("password").setValue(state.password)
                            }
                        }
                    },
                    onBackClick = { currentSubScreen = "Main" }
                )
            }
            "Accessibility" -> {
                AccessibilityHubScreen(
                    initialFontSize = fontSize,
                    initialHighContrast = highContrast,
                    onFontSizeChange = onFontSizeChange,
                    onHighContrastChange = onHighContrastChange,
                    onBackClick = { currentSubScreen = "Main" }
                )
            }
            "Main" -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp)
                    ) {
                        Spacer(Modifier.height(54.dp))

                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Filled.Settings, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(26.dp))
                                Text("Settings", fontSize = 24.scaled(fontSize), fontWeight = FontWeight.ExtraBold, color = ElderlyColors.NavyBlue)
                            }
                            Text("Manage your account and system configurations", fontSize = 14.scaled(fontSize), color = ElderlyColors.GrayMuted)
                        }

                        Spacer(Modifier.height(22.dp))

                        // ══════════════════════════════════════════════════════════════════
                        //  1. PROFILE SUMMARY
                        // ══════════════════════════════════════════════════════════════════
                        SettingsSectionLabel("MY PROFILE")

                        ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { currentSubScreen = "Profile" }
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(60.dp).background(ElderlyColors.NavySurface, CircleShape).border(2.dp, ElderlyColors.NavyBlue.copy(0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(32.dp))
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(Modifier.weight(1f)) {
                                    // 🟢 Smart Profile Validator
                                    val isProfileIncomplete = profileName.isBlank() || profilePhone.isBlank() || profileAge.isBlank() || profileGender == Gender.Unset

                                    if (isProfileIncomplete) {
                                        Text(if (profileName.isNotBlank()) profileName else "Profile Incomplete", fontSize = 18.scaled(fontSize), fontWeight = FontWeight.ExtraBold, color = ElderlyColors.PrimaryText)
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Error, null, tint = ElderlyColors.Crimson, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Tap to complete your info", fontSize = 13.scaled(fontSize), color = ElderlyColors.Crimson, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(profileName, fontSize = 18.scaled(fontSize), fontWeight = FontWeight.ExtraBold, color = ElderlyColors.PrimaryText)
                                        Text("$profileAge Years Old  ·  ${profileGender.name}", fontSize = 14.scaled(fontSize), color = ElderlyColors.GrayMuted)
                                        Text(profilePhone, fontSize = 13.scaled(fontSize), color = ElderlyColors.GrayMuted)
                                    }
                                }

                                Box(modifier = Modifier.size(38.dp).background(ElderlyColors.NavySurface, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.ChevronRight, "Edit profile", tint = ElderlyColors.NavyBlue, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        // ══════════════════════════════════════════════════════════════════
                        //  2. FALL & ALERT SETTINGS
                        // ══════════════════════════════════════════════════════════════════
                        SettingsSectionLabel("FALL SAFETY SETTINGS")

                        ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Column(modifier = Modifier.padding(20.dp)) {

                                val sensitivityLabel = when (fallSensitivity) {
                                    0f   -> "Low — Fewer false alarms"
                                    2f   -> "High — Ultra sensitive"
                                    else -> "Normal — Recommended"
                                }
                                val sensitivityColor = when (fallSensitivity) {
                                    0f   -> ElderlyColors.HeartbeatGreen
                                    2f   -> ElderlyColors.Crimson
                                    else -> ElderlyColors.NavyBlue
                                }

                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(Modifier.size(42.dp).background(sensitivityColor.copy(0.12f), RoundedCornerShape(12.dp)), Alignment.Center) {
                                            Icon(Icons.Filled.Sensors, null, tint = sensitivityColor, modifier = Modifier.size(22.dp))
                                        }
                                        Column {
                                            Text("Fall Detection Sensitivity", fontSize = 15.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
                                            Text("Adjust based on your daily activity level", fontSize = 12.scaled(fontSize), color = ElderlyColors.GrayMuted)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Box(modifier = Modifier.background(sensitivityColor.copy(0.12f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                                    Text(sensitivityLabel, fontSize = 13.scaled(fontSize), color = sensitivityColor, fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    listOf("Low","Normal","High").forEach { lbl ->
                                        Text(lbl, fontSize = 12.scaled(fontSize), color = ElderlyColors.GrayMuted)
                                    }
                                }

                                Slider(
                                    value         = fallSensitivity,
                                    onValueChange = onFallSensitivityChange,
                                    valueRange    = 0f..2f,
                                    steps         = 1,
                                    modifier      = Modifier.fillMaxWidth(),
                                    colors        = SliderDefaults.colors(thumbColor = sensitivityColor, activeTrackColor = sensitivityColor, inactiveTrackColor = ElderlyColors.GrayLight)
                                )

                                Spacer(Modifier.height(20.dp))
                                HorizontalDivider(color = ElderlyColors.GrayLight, thickness = 0.5.dp)
                                Spacer(Modifier.height(20.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(42.dp).background(ElderlyColors.NavySurface, RoundedCornerShape(12.dp)), Alignment.Center) {
                                        Icon(Icons.Filled.Timer, null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Text("Countdown Timer", fontSize = 15.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
                                        Text("Time window before help is dispatched", fontSize = 12.scaled(fontSize), color = ElderlyColors.GrayMuted)
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                                    listOf(15 to "15 Seconds\nStandard", 30 to "30 Seconds\nExtended").forEach { (secs, label) ->
                                        val sel = countdownDuration == secs
                                        Box(
                                            modifier = Modifier.weight(1f).height(72.dp)
                                                .border(2.dp, if (sel) ElderlyColors.NavyBlue else ElderlyColors.GrayBorder, RoundedCornerShape(16.dp))
                                                .background(if (sel) ElderlyColors.NavySurface else ElderlyColors.Surface, RoundedCornerShape(16.dp))
                                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCountdownDurationChange(secs) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontSize = 15.scaled(fontSize), fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal, color = if (sel) ElderlyColors.NavyBlue else ElderlyColors.GrayMuted, textAlign = TextAlign.Center, lineHeight = 20.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        // ══════════════════════════════════════════════════════════════════
                        //  3. SYSTEM HEALTH CHECK
                        // ══════════════════════════════════════════════════════════════════
                        SettingsSectionLabel("SYSTEM DIAGNOSTICS")

                        ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Column {
                                SystemHealthRow(icon = Icons.Filled.VolumeUp, label = "System Volume", status = if (volumeIsLoud) "Loud — Audio alerts active" else "Muted — Please increase device volume", ok = volumeIsLoud, fontSize = fontSize)
                                HorizontalDivider(color = ElderlyColors.GrayLight, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                SystemHealthRow(icon = Icons.Filled.LocationOn, label = "GPS Tracking", status = if (gpsEnabled) "Location Active — Caregivers can track safely" else "GPS Muted — Please enable location permission", ok = gpsEnabled, fontSize = fontSize)
                                HorizontalDivider(color = ElderlyColors.GrayLight, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                SystemHealthRow(icon = Icons.Filled.BatteryAlert, label = "Battery Level", status = if (batteryOk) "Battery Healthy — Above 20%" else "Battery Low — Please charge device immediately", ok = batteryOk, fontSize = fontSize)
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        // ══════════════════════════════════════════════════════════════════
                        //  4. ACCESSIBILITY HUB LINK
                        // ══════════════════════════════════════════════════════════════════
                        SettingsSectionLabel("ACCESSIBILITY")

                        ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { currentSubScreen = "Accessibility" }.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp).background(ElderlyColors.NavySurface, RoundedCornerShape(13.dp)), Alignment.Center) {
                                    Icon(Icons.Filled.Accessibility, null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Accessibility Hub", fontSize = 16.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
                                    Text("Text size, high contrast display options", fontSize = 13.scaled(fontSize), color = ElderlyColors.GrayMuted)
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = ElderlyColors.GrayMuted, modifier = Modifier.size(22.dp))
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // ══════════════════════════════════════════════════════════════════
                        //  5. LOGOUT SAFEGUARD
                        // ══════════════════════════════════════════════════════════════════
                        SettingsSectionLabel("ACCOUNT DISCONNECTION")

                        ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showLogoutDialog = true }.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp).background(ElderlyColors.CrimsonLight, RoundedCornerShape(13.dp)), Alignment.Center) {
                                    Icon(Icons.Filled.ExitToApp, null, tint = ElderlyColors.Crimson, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Log Out", fontSize = 16.scaled(fontSize), fontWeight = FontWeight.Bold, color = ElderlyColors.Crimson)
                                    Text("Fall detection monitoring will be deactivated", fontSize = 13.scaled(fontSize), color = ElderlyColors.GrayMuted)
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = ElderlyColors.Crimson, modifier = Modifier.size(22.dp))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    if (showLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            containerColor   = ElderlyColors.Surface,
                            shape            = RoundedCornerShape(24.dp),
                            icon = {
                                Box(Modifier.size(56.dp).background(ElderlyColors.CrimsonLight, CircleShape), Alignment.Center) {
                                    Icon(Icons.Filled.Warning, null, tint = ElderlyColors.Crimson, modifier = Modifier.size(28.dp))
                                }
                            },
                            title = { Text("Log Out?", fontSize = 20.scaled(fontSize), fontWeight = FontWeight.ExtraBold, color = ElderlyColors.PrimaryText, textAlign = TextAlign.Center) },
                            text = { Text("If you log out, fall detection monitoring will be STOPPED. Your caregiver will not receive alerts until you log back in.\n\nAre you sure you want to log out?", fontSize = 15.scaled(fontSize), color = ElderlyColors.SecondaryText, lineHeight = 22.sp, textAlign = TextAlign.Center) },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showLogoutDialog = false },
                                    shape   = RoundedCornerShape(14.dp),
                                    border  = BorderStroke(1.dp, ElderlyColors.GrayBorder),
                                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = ElderlyColors.GrayMuted)
                                ) { Text("Cancel — Keep Safe", fontSize = 15.scaled(fontSize), fontWeight = FontWeight.Bold) }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showLogoutDialog = false; onLogout() },
                                    shape   = RoundedCornerShape(14.dp),
                                    colors  = ButtonDefaults.buttonColors(containerColor = ElderlyColors.Crimson)
                                ) { Text("Yes, Log Out", fontSize = 15.scaled(fontSize), fontWeight = FontWeight.ExtraBold, color = Color.White) }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EDIT PROFILE SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    initialState: ProfileFormState = ProfileFormState(),
    onSaveClick:  (ProfileFormState) -> Unit = {},
    onBackClick:  () -> Unit = {},
) {
    var name           by remember { mutableStateOf(initialState.name) }
    var phone          by remember { mutableStateOf(initialState.phone) }
    var age            by remember { mutableStateOf(initialState.age) }
    var gender         by remember { mutableStateOf(initialState.gender) }
    var password       by remember { mutableStateOf(initialState.password) }
    var retypePassword by remember { mutableStateOf(initialState.retypePassword) }

    var nameError       by remember { mutableStateOf<String?>(null) }
    var phoneError      by remember { mutableStateOf<String?>(null) }
    var ageError        by remember { mutableStateOf<String?>(null) }
    var genderError     by remember { mutableStateOf<String?>(null) }
    var passwordError   by remember { mutableStateOf<String?>(null) }
    var retypeError     by remember { mutableStateOf<String?>(null) }

    var saveSuccess     by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        nameError       = if (name.isBlank()) "Please enter your name" else null
        phoneError      = if (phone.length < 8 || !phone.all { it.isDigit() || it == '+' || it == '-' }) "Enter a valid phone number" else null

        // 🟢 FIXED: No more logic traps! Simple, clean checks.
        val ageInt = age.trim().toIntOrNull()
        ageError = when {
            ageInt == null -> "Enter a valid age"
            ageInt !in 1..120 -> "Age must be between 1–120"
            else -> null
        }

        genderError     = if (gender == Gender.Unset) "Please select a gender" else null
        passwordError   = if (password.isNotEmpty() && password.length < 6) "Password must be at least 6 characters" else null
        retypeError     = if (password != retypePassword) "Passwords do not match" else null

        return listOf(nameError, phoneError, ageError, genderError, passwordError, retypeError).all { it == null }
    }

    Scaffold(
        containerColor = ElderlyColors.Background,
        topBar = { EditProfileTopBar(onBackClick = onBackClick) },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProfileTextField(label = "Full Name", value = name, onValueChange = { name = it; nameError = null }, placeholder = "e.g. Ahmad Razali", error = nameError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            ProfileTextField(label = "Phone Number", value = phone, onValueChange = { phone = it; phoneError = null }, placeholder = "e.g. 0123456789", error = phoneError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

            // 🟢 FIXED: Smart filter instantly strips spaces and non-digits as you type
            ProfileTextField(
                label = "Age",
                value = age,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    if (filtered.length <= 3) { age = filtered; ageError = null }
                },
                placeholder = "e.g. 72",
                error = ageError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            GenderPickerSection(selected = gender, onSelect = { gender = it; genderError = null }, error = genderError)

            PasswordField(label = "New Password (Optional)", value = password, onValueChange = { password = it; passwordError = null; retypeError = null }, error = passwordError)
            PasswordField(label = "Retype Password", value = retypePassword, onValueChange = { retypePassword = it; retypeError = null }, error = retypeError)

            if (saveSuccess) {
                SaveSuccessBanner()
            }

            Spacer(modifier = Modifier.height(4.dp))

            SaveProfileButton(
                onClick = {
                    saveSuccess = false
                    if (validate()) {
                        saveSuccess = true
                        onSaveClick(ProfileFormState(name, phone, age, gender, password, retypePassword))
                    }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPER UI COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ElderlyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.Surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), content = content)
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.GrayMuted, letterSpacing = 1.4.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
}

@Composable
private fun SystemHealthRow(icon: ImageVector, label: String, status: String, ok: Boolean, fontSize: AppFontSize) {
    val statusColor = if (ok) ElderlyColors.HeartbeatGreen else ElderlyColors.Crimson
    val iconBg      = if (ok) ElderlyColors.HeartbeatGreen.copy(alpha = 0.12f) else ElderlyColors.CrimsonLight

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(iconBg, RoundedCornerShape(12.dp)), Alignment.Center) { Icon(icon, null, tint = statusColor, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.scaled(fontSize), fontWeight = FontWeight.SemiBold, color = ElderlyColors.PrimaryText)
            Text(status, fontSize = 12.scaled(fontSize), color = statusColor, fontWeight = FontWeight.Medium)
        }
        Box(modifier = Modifier.background(iconBg, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(if (ok) "OK ✓" else "Warning!", fontSize = 11.scaled(fontSize), color = statusColor, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.NavyBlue) },
        navigationIcon = {
            IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back to settings", tint = ElderlyColors.NavyBlue, modifier = Modifier.size(28.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ElderlyColors.Surface)
    )
}

@Composable
private fun ProfileTextField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, error: String?, keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    val borderColor = when {
        error != null -> ElderlyColors.Crimson
        value.isNotEmpty() -> ElderlyColors.NavyBlue
        else -> ElderlyColors.GrayLight
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ElderlyColors.NavyBlue)
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
            placeholder = { Text(text = placeholder, fontSize = 17.sp, color = ElderlyColors.GrayMuted) },
            textStyle = LocalTextStyle.current.copy(fontSize = 19.sp, fontWeight = FontWeight.Medium, color = ElderlyColors.NavyBlue),
            singleLine = true, isError = error != null, keyboardOptions = keyboardOptions, shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElderlyColors.NavyBlue, unfocusedBorderColor = borderColor, errorBorderColor = ElderlyColors.Crimson,
                focusedContainerColor = ElderlyColors.NavySurface, unfocusedContainerColor = ElderlyColors.Surface, cursorColor = ElderlyColors.NavyBlue
            )
        )
        if (error != null) Text(text = "⚠ $error", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ElderlyColors.Crimson, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit, error: String?) {
    var passwordVisible by remember { mutableStateOf(false) }
    val borderColor = when {
        error != null -> ElderlyColors.Crimson
        value.isNotEmpty() -> ElderlyColors.NavyBlue
        else -> ElderlyColors.GrayLight
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ElderlyColors.NavyBlue)
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
            placeholder = { Text(text = "Enter password", fontSize = 17.sp, color = ElderlyColors.GrayMuted) },
            textStyle = LocalTextStyle.current.copy(fontSize = 19.sp, fontWeight = FontWeight.Medium, color = ElderlyColors.NavyBlue),
            singleLine = true, isError = error != null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = ElderlyColors.NavyBlue, modifier = Modifier.size(28.dp))
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElderlyColors.NavyBlue, unfocusedBorderColor = borderColor, errorBorderColor = ElderlyColors.Crimson,
                focusedContainerColor = ElderlyColors.NavySurface, unfocusedContainerColor = ElderlyColors.Surface, cursorColor = ElderlyColors.NavyBlue
            )
        )
        if (error != null) Text(text = "⚠ $error", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ElderlyColors.Crimson, modifier = Modifier.padding(start = 4.dp))
    }
}

// 🟢 FIXED: Professional Icons instead of Emojis
@Composable
private fun GenderPickerSection(selected: Gender, onSelect: (Gender) -> Unit, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Gender", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ElderlyColors.NavyBlue)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GenderCard(icon = Icons.Filled.Man, label = "Male", isSelected = selected == Gender.Male, onClick = { onSelect(Gender.Male) }, modifier = Modifier.weight(1f), hasError = error != null && selected == Gender.Unset)
            GenderCard(icon = Icons.Filled.Woman, label = "Female", isSelected = selected == Gender.Female, onClick = { onSelect(Gender.Female) }, modifier = Modifier.weight(1f), hasError = error != null && selected == Gender.Unset)
        }
        if (error != null) Text(text = "⚠ $error", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ElderlyColors.Crimson, modifier = Modifier.padding(start = 4.dp))
    }
}

// 🟢 FIXED: Takes ImageVector instead of String emoji
@Composable
private fun GenderCard(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, hasError: Boolean = false) {
    val bgColor by animateColorAsState(targetValue = if (isSelected) ElderlyColors.NavyBlue else ElderlyColors.Surface, animationSpec = tween(durationMillis = 220), label = "gender_bg_$label")
    val contentColor = if (isSelected) Color.White else ElderlyColors.NavyBlue
    val borderColor = when {
        hasError -> ElderlyColors.Crimson
        isSelected -> ElderlyColors.NavyBlue
        else -> ElderlyColors.GrayLight
    }
    val borderWidth = if (isSelected) 2.5.dp else 1.5.dp

    Box(
        modifier = modifier.height(110.dp).clip(RoundedCornerShape(16.dp)).background(bgColor).clickable(onClick = onClick).border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(36.dp))
            Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
private fun SaveSuccessBanner() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = ElderlyColors.HeartbeatGreen.copy(alpha = 0.12f)), border = BorderStroke(1.dp, ElderlyColors.HeartbeatGreen)) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "✅", fontSize = 22.sp)
            Text(text = "Profile saved successfully!", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ElderlyColors.HeartbeatGreen)
        }
    }
}

@Composable
private fun SaveProfileButton(onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ElderlyColors.NavyBlue, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(text = "SAVE PROFILE", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}