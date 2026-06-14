package com.group.talihayat.ui.elderly

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group.talihayat.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditElderlyMedicalScreen(onSaveClick: () -> Unit, onBackClick: () -> Unit) {
    // ── Emergency Contacts State (From image_3.png) ──
    var contactName1 by remember { mutableStateOf("Siti Nurhaliza") }
    var contactPhone1 by remember { mutableStateOf("+60 12-345 6789") }

    var contactName2 by remember { mutableStateOf("Dr. Hafiz Rahman") }
    var contactPhone2 by remember { mutableStateOf("+60 3-2691 0000") }

    var contactName3 by remember { mutableStateOf("Ahmad Jr.") }
    var contactPhone3 by remember { mutableStateOf("+60 11-234 5678") }

    // ── Medical Profile State (From image_4.png) ──
    var bloodType by remember { mutableStateOf("B+") }
    var allergies by remember { mutableStateOf("Penicillin, Shellfish") }
    var conditions by remember { mutableStateOf("Hypertension, Type 2 Diabetes") }
    var medications by remember { mutableStateOf("Metformin 500mg, Amlodipine 5mg") }
    var lastCheckup by remember { mutableStateOf("15 May 2025") }

    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // ── TOP BAR AREA ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(Surface, RoundedCornerShape(12.dp)).border(1.dp, GrayLight, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = Navy, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text("Medical & Contacts Setup", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
        }

        // ── SCROLLABLE FORM ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text("Keep this medical history updated so your caregiver can easily view it in an emergency.", fontSize = 13.sp, color = GrayMuted, lineHeight = 18.sp)
            Spacer(Modifier.height(24.dp))

            // 📜 SECTION 1: EMERGENCY CONTACTS
            ElderlySectionHeader(title = "Emergency Contacts")
            ElderlyFormCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ElderlyInputField(value = contactName1, onValueChange = { contactName1 = it }, label = "Primary Caregiver Name", icon = Icons.Outlined.Person)
                    ElderlyInputField(value = contactPhone1, onValueChange = { contactPhone1 = it }, label = "Primary Caregiver Phone", icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone)
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    ElderlyInputField(value = contactName2, onValueChange = { contactName2 = it }, label = "Physician / Doctor Name", icon = Icons.Outlined.LocalHospital)
                    ElderlyInputField(value = contactPhone2, onValueChange = { contactPhone2 = it }, label = "Physician Phone", icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone)
                    HorizontalDivider(color = GrayBorder, thickness = 0.5.dp)
                    ElderlyInputField(value = contactName3, onValueChange = { contactName3 = it }, label = "Next of Kin Name", icon = Icons.Outlined.People)
                    ElderlyInputField(value = contactPhone3, onValueChange = { contactPhone3 = it }, label = "Next of Kin Phone", icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 📜 SECTION 2: MEDICAL PROFILE
            //Make space for health details
                    ElderlySectionHeader(title = "Medical Profile")
            ElderlyFormCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ElderlyInputField(value = bloodType, onValueChange = { bloodType = it }, label = "Blood Type", icon = Icons.Outlined.Bloodtype)
                    ElderlyInputField(value = allergies, onValueChange = { allergies = it }, label = "Allergies", icon = Icons.Outlined.Warning)
                    ElderlyInputField(value = conditions, onValueChange = { conditions = it }, label = "Underlying Conditions", icon = Icons.Outlined.MedicalInformation)
                    ElderlyInputField(value = medications, onValueChange = { medications = it }, label = "Current Medications", icon = Icons.Outlined.Medication)
                    ElderlyInputField(value = lastCheckup, onValueChange = { lastCheckup = it }, label = "Last Medical Check-up Date", icon = Icons.Outlined.CalendarMonth)
                }
            }

            Spacer(Modifier.height(40.dp))

            // 💾 SAVE BUTTON
            Button(
                onClick = {
                    isSaving = true
                    // In production, this saves to Firebase Database
                    onSaveClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp), // Extra thick target for senior touch precision
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── ELDERLY SPECIFIC SUB-COMPONENTS ──

@Composable
private fun ElderlySectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = GrayMuted,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun ElderlyFormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun ElderlyInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(label, fontSize = 13.sp, color = Navy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            leadingIcon = { Icon(icon, null, tint = Teal, modifier = Modifier.size(22.dp)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = InputBorder,
                // Change containerColor to these two lines:
                focusedContainerColor = Background,
                unfocusedContainerColor = Background
            )
        )
    }
}