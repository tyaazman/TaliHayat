package com.group.talihayat.ui.caretaker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group.talihayat.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    // State Handlers
    var biometricLockEnabled by remember { mutableStateOf(false) }
    var twoFactorEnabled by remember { mutableStateOf(true) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // ── 1. TOP NAVIGATION BAR ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, GrayLight, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = Navy,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Privacy & Security",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy
            )
        }

        // ── 2. SCROLLABLE CONTENT AREA ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Configure your security preferences and manage how your healthcare and sensor data is handled.",
                fontSize = 13.sp,
                color = GrayMuted,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(24.dp))

            // 🔐 CATEGORY 1: ACCESS CONTROL
            SecuritySectionHeader("Access Control")
            SecurityCard {
                Column {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Fingerprint,
                        label = "Biometric Authentication",
                        subtitle = "Require fingerprint or Face ID to open app",
                        checked = biometricLockEnabled,
                        onToggle = { biometricLockEnabled = it }
                    )
                    HorizontalDivider(color = GrayLight, thickness = 0.5.dp)
                    SettingsNavRow(
                        icon = Icons.Outlined.Lock,
                        label = "Change App PIN",
                        subtitle = "Configure your fallback 4-digit security code",
                        onClick = { /* Handle PIN modification */ }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 🛡️ CATEGORY 2: ACCOUNT PROTECTION
            SecuritySectionHeader("Account Protection")
            SecurityCard {
                Column {
                    SettingsToggleRow(
                        icon = Icons.Outlined.EnhancedEncryption,
                        label = "Two-Factor Authentication",
                        subtitle = "Receive secure verification code on login",
                        checked = twoFactorEnabled,
                        onToggle = { twoFactorEnabled = it }
                    )
                    HorizontalDivider(color = GrayLight, thickness = 0.5.dp)
                    SettingsNavRow(
                        icon = Icons.Outlined.Devices,
                        label = "Trusted Devices",
                        subtitle = "Monitor active smartphone & hardware sessions",
                        onClick = { /* Handle session viewer navigation */ }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ⚙️ CATEGORY 3: DATA GOVERNANCE & PERMISSIONS
            SecuritySectionHeader("Data Governance")
            SecurityCard {
                Column {
                    SettingsNavRow(
                        icon = Icons.Outlined.ManageAccounts,
                        label = "Device Permissions",
                        subtitle = "Manage background location & camera system access",
                        onClick = {
                            // High-value native intent opening standard Android system settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(color = GrayLight, thickness = 0.5.dp)
                    SettingsNavRow(
                        icon = Icons.Outlined.DeleteSweep,
                        label = "Clear App Cache",
                        subtitle = "Wipe local temporary log assets & images",
                        onClick = { /* Execute native cache clearance runtime */ }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 📄 CATEGORY 4: LEGAL & COMPLIANCE
            SecuritySectionHeader("Compliance")
            SecurityCard {
                Column {
                    SettingsNavRow(
                        icon = Icons.Outlined.Description,
                        label = "Privacy Policy",
                        subtitle = "Read how TaliHayat encrypts the elderly telemetry",
                        onClick = { showPrivacyDialog = true }
                    )
                    HorizontalDivider(color = GrayLight, thickness = 0.5.dp)
                    SettingsNavRow(
                        icon = Icons.Outlined.NoAccounts,
                        label = "Delete Account & Data",
                        subtitle = "Permanently purge registration history from Firebase",
                        tint = Crimson,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    // ── 3. POLICY MODAL OVERLAY ──
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = Navy) },
            text = {
                Text(
                    "TaliHayat secures all the elderly's data using end-to-end encryption. Real-time sensor metrics (IMU analytics) are strictly evaluated locally or passed via structured parameters to safe Firebase environments. No camera streams are shared or distributed without explicit caregiver credentials.",
                    fontSize = 14.sp, color = GrayMuted, lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood", color = Teal, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Surface
        )
    }

    // ── 4. ACCOUNT DELETION OVERLAY ──
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Purge Account Data?", fontWeight = FontWeight.Bold, color = Crimson) },
            text = { Text("This operation is permanent. All historical tracking logs, medical profiles, and structural linkage pairs will be instantly dropped from our cloud infrastructure.", fontSize = 14.sp, color = GrayMuted) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Delete Everything", color = Crimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Navy)
                }
            },
            containerColor = Surface
        )
    }
}

// ── SUB-COMPONENT WRAPPERS FOR CLEAN HIERARCHY ──

@Composable
private fun SecuritySectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = GrayMuted,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SecurityCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp),
        content = { content() }
    )
}

@Composable
private fun SettingsToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(GrayLight, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Navy, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(subtitle, fontSize = 12.sp, color = GrayMuted)
        }
        Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Surface, checkedTrackColor = Teal))
    }
}

@Composable
private fun SettingsNavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, subtitle: String, tint: Color = Navy, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(GrayLight, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = tint)
                Text(subtitle, fontSize = 12.sp, color = GrayMuted)
            }
        }
    }
}