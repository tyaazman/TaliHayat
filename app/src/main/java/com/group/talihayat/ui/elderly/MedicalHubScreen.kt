package com.group.talihayat.ui.elderly

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.group.talihayat.ui.theme.*
import android.app.DatePickerDialog
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CalendarMonth

// ─────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────

private data class Medicine(
    val id        : Int,
    val name      : String,
    val dosage    : String,
    val type      : String,
    val isPrn     : Boolean,
    val slot      : String,
    val startDate : String,
    val endDate   : String
)

private fun getUnitForType(type: String): String {
    return when (type) {
        "Pill" -> "pills"
        "Capsule" -> "capsules"
        "Liquid" -> "ml"
        "Injection" -> "units"
        "Inhaler" -> "puffs"
        "Drops" -> "drops"
        "Patch" -> "patches"
        "Cream", "Ointment" -> "applications"
        else -> ""
    }
}

private fun getIconForType(type: String): ImageVector {
    return when (type) {
        "Liquid", "Drops" -> Icons.Filled.WaterDrop
        "Inhaler" -> Icons.Filled.Air
        "Injection" -> Icons.Filled.Vaccines
        "Cream", "Ointment" -> Icons.Filled.LocalPharmacy
        "Patch" -> Icons.Filled.Healing
        else -> Icons.Filled.Medication
    }
}

private enum class MedEditMode {
    NONE, MEDICINE, BLOOD_TYPE, ALLERGIES, CONDITIONS, CHECKUP
}

private val BloodTypes = listOf("A+","A−","B+","B−","AB+","AB−","O+","O−")
private val QuickAllergies  = listOf("Penicillin", "Seafood", "Sulfa", "Latex", "Dust Mites", "Pollen")
private val QuickConditions = listOf("Hypertension", "Diabetes", "Asthma", "Arthritis", "Osteoporosis", "Heart Disease", "Kidney Disease", "Stroke")

// ═══════════════════════════════════════════════════════════════════════════════
//  MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalHubScreen(fontSize: AppFontSize) {
    val database = FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    val takenMap = remember { mutableStateMapOf<Int, Boolean>() }
    var bloodType       by remember { mutableStateOf("") } // 🟢 Blank default
    val allergies       = remember { mutableStateListOf<String>() }
    val medicines       = remember { mutableStateListOf<Medicine>() }
    val conditions      = remember { mutableStateListOf<String>() }
    var lastCheckDate   by remember { mutableStateOf("") } // 🟢 Blank default
    var lastCheckClinic by remember { mutableStateOf("") } // 🟢 Blank default

    var editMode       by remember { mutableStateOf(MedEditMode.NONE) }
    var editingMedName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUid) {
        if (currentUid != null) {
            val medRef = database.child("users").child(currentUid).child("medical_profile")

            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            medRef.child("taken_logs").child(todayStr).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    takenMap.clear()
                    snap.children.forEach { child ->
                        val medId = child.key?.toIntOrNull()
                        val isTaken = child.getValue(Boolean::class.java) ?: false
                        if (medId != null) takenMap[medId] = isTaken
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            medRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        bloodType = snapshot.child("blood_type").getValue(String::class.java) ?: ""
                        lastCheckDate = snapshot.child("last_check_date").getValue(String::class.java) ?: ""
                        lastCheckClinic = snapshot.child("last_check_clinic").getValue(String::class.java) ?: ""

                        val dbAllergies = snapshot.child("allergies").children.mapNotNull { it.getValue(String::class.java) }
                        allergies.clear()
                        if (dbAllergies.isNotEmpty()) allergies.addAll(dbAllergies)

                        val dbConditions = snapshot.child("conditions").children.mapNotNull { it.getValue(String::class.java) }
                        conditions.clear()
                        if (dbConditions.isNotEmpty()) conditions.addAll(dbConditions)

                        val dbMeds = snapshot.child("medicines").children.mapNotNull { medSnap ->
                            val id = medSnap.child("id").getValue(Int::class.java) ?: return@mapNotNull null
                            val name = medSnap.child("name").getValue(String::class.java) ?: ""
                            val dosage = medSnap.child("dosage").getValue(String::class.java) ?: ""
                            val type = medSnap.child("type").getValue(String::class.java) ?: "Pill"
                            val isPrn = medSnap.child("isPrn").getValue(Boolean::class.java) ?: false
                            val slot = medSnap.child("slot").getValue(String::class.java) ?: ""
                            val startDate = medSnap.child("startDate").getValue(String::class.java) ?: ""
                            val endDate = medSnap.child("endDate").getValue(String::class.java) ?: ""
                            Medicine(id, name, dosage, type, isPrn, slot, startDate, endDate)
                        }

                        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        val today = java.util.Date()

                        val activeMeds = dbMeds.filter { med ->
                            if (med.endDate.isBlank()) return@filter true
                            try {
                                val end = dateFormat.parse(med.endDate)
                                end == null || !end.before(today)
                            } catch (e: Exception) { true }
                        }

                        medicines.clear()
                        medicines.addAll(activeMeds)

                        if (activeMeds.size != dbMeds.size) {
                            currentUid?.let { database.child("users").child(it).child("medical_profile").child("medicines").setValue(activeMeds) }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Spacer(Modifier.height(54.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.LocalHospital, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(26.dp))
                    Text("Medical Hub", fontSize = 24.scaled(fontSize), fontWeight = FontWeight.ExtraBold, color = ElderlyColors.NavyBlue)
                }
                Text("Review your medications and health records", fontSize = 14.sp, color = ElderlyColors.GrayMuted)
            }

            Spacer(Modifier.height(20.dp))

            MedSectionLabel("TODAY'S MEDICATIONS")

            if (medicines.isEmpty()) {
                ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(64.dp).background(ElderlyColors.NavySurface, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Medication, contentDescription = null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No Medications Recorded", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ElderlyColors.PrimaryText)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap the 'Add Medicine Information' button below to add your daily prescriptions and set up reminders.", fontSize = 14.sp, color = ElderlyColors.GrayMuted, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    }
                }
            } else {
                MedicationTimeBlock("Morning (6am – 11am)", "morning", medicines, takenMap, fontSize, Icons.Filled.WbSunny, Modifier.padding(horizontal = 20.dp), database, currentUid) { medName -> editingMedName = medName; editMode = MedEditMode.MEDICINE }
                Spacer(Modifier.height(12.dp))
                MedicationTimeBlock("Afternoon (12pm – 5pm)", "afternoon", medicines, takenMap, fontSize, Icons.Filled.WbCloudy, Modifier.padding(horizontal = 20.dp), database, currentUid) { medName -> editingMedName = medName; editMode = MedEditMode.MEDICINE }
                Spacer(Modifier.height(12.dp))
                MedicationTimeBlock("Night (6pm – 10pm)", "night", medicines, takenMap, fontSize, Icons.Filled.NightsStay, Modifier.padding(horizontal = 20.dp), database, currentUid) { medName -> editingMedName = medName; editMode = MedEditMode.MEDICINE }
                Spacer(Modifier.height(12.dp))
                MedicationTimeBlock("As Needed (PRN)", "prn", medicines, takenMap, fontSize, Icons.Filled.MedicalServices, Modifier.padding(horizontal = 20.dp), database, currentUid) { medName -> editingMedName = medName; editMode = MedEditMode.MEDICINE }
            }
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(62.dp)
                    .background(Brush.horizontalGradient(listOf(ElderlyColors.NavyBlue, Color(0xFF2D5288))), RoundedCornerShape(18.dp))
                    .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = ElderlyColors.NavyBlue.copy(0.22f), spotColor = ElderlyColors.NavyBlue.copy(0.22f))
                    .clickable {
                        editingMedName = null
                        editMode = MedEditMode.MEDICINE
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Text("Add Medicine Information", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(28.dp))

            MedSectionLabel("EMERGENCY PASSPORT")
            Text("This crucial information is visible to medical personnel during emergencies.", fontSize = 13.sp, color = ElderlyColors.GrayMuted, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Spacer(Modifier.height(12.dp))

            ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(60.dp).background(ElderlyColors.CrimsonLight, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Bloodtype, null, tint = ElderlyColors.Crimson, modifier = Modifier.size(20.dp))
                            Text(if (bloodType.isBlank()) "?" else bloodType, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ElderlyColors.Crimson)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Blood Type", fontSize = 12.sp, color = ElderlyColors.GrayMuted, fontWeight = FontWeight.Medium)
                        Text(if (bloodType.isBlank()) "Not Set" else bloodType, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if(bloodType.isBlank()) ElderlyColors.GrayMuted else ElderlyColors.PrimaryText)
                        Text("Verified medical data", fontSize = 12.sp, color = ElderlyColors.GrayMuted)
                    }
                    IconButton(onClick = { editMode = MedEditMode.BLOOD_TYPE }) { Icon(Icons.Outlined.Edit, "Edit blood type", tint = ElderlyColors.GrayMuted) }
                }
            }

            Spacer(Modifier.height(12.dp))

            ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = ElderlyColors.AmberWarning, modifier = Modifier.size(20.dp))
                            Text("Allergies", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
                        }
                        IconButton(onClick = { editMode = MedEditMode.ALLERGIES }) { Icon(Icons.Outlined.Edit, "Edit allergies", tint = ElderlyColors.GrayMuted) }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (allergies.isEmpty()) {
                        Text("No data recorded. Tap the edit icon to add allergies.", fontSize = 14.sp, color = ElderlyColors.GrayMuted, modifier = Modifier.padding(top = 4.dp))
                    } else if (allergies.size == 1 && allergies[0] == "None") {
                        Box(modifier = Modifier.fillMaxWidth().background(ElderlyColors.HeartbeatGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).padding(14.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.CheckCircle, null, tint = ElderlyColors.HeartbeatGreen, modifier = Modifier.size(18.dp))
                                Text("NO RECORDED ALLERGIES", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ElderlyColors.HeartbeatGreen)
                            }
                        }
                    } else {
                        FlowChips(items = allergies, chipColor = ElderlyColors.Crimson, chipBg = ElderlyColors.CrimsonLight)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalHospital, null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(20.dp))
                            Text("Chronic Conditions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.PrimaryText)
                        }
                        IconButton(onClick = { editMode = MedEditMode.CONDITIONS }) { Icon(Icons.Outlined.Edit, "Edit conditions", tint = ElderlyColors.GrayMuted) }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (conditions.isEmpty()) {
                        Text("No data recorded. Tap the edit icon to add medical conditions.", fontSize = 14.sp, color = ElderlyColors.GrayMuted, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        FlowChips(items = conditions, chipColor = ElderlyColors.NavyBlue, chipBg = ElderlyColors.NavySurface)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ElderlyCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(ElderlyColors.NavySurface, RoundedCornerShape(13.dp)), Alignment.Center) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Last Medical Check-up", fontSize = 12.sp, color = ElderlyColors.GrayMuted)
                        Text(if(lastCheckDate.isBlank()) "Not Set" else lastCheckDate, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if(lastCheckDate.isBlank()) ElderlyColors.GrayMuted else ElderlyColors.PrimaryText)
                        if(lastCheckClinic.isNotBlank()) {
                            Text(lastCheckClinic, fontSize = 13.sp, color = ElderlyColors.GrayMuted)
                        }
                    }
                    IconButton(onClick = { editMode = MedEditMode.CHECKUP }) { Icon(Icons.Outlined.Edit, "Edit checkup", tint = ElderlyColors.GrayMuted) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        AnimatedVisibility(
            visible = editMode != MedEditMode.NONE,
            enter   = slideInVertically(initialOffsetY = { it }, animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(durationMillis = 250)),
            exit    = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            MedEditWorkspace(
                mode            = editMode,
                editingMedName  = editingMedName,
                allMeds         = medicines,
                bloodType       = bloodType,
                allergies       = allergies,
                conditions      = conditions,
                lastCheckDate   = lastCheckDate,
                lastCheckClinic = lastCheckClinic,
                onBloodTypeChange   = { bloodType = it; currentUid?.let { uid -> database.child("users").child(uid).child("medical_profile").child("blood_type").setValue(it) } },
                onAllergyAdd    = { if (!allergies.contains(it)) { allergies.remove("None"); allergies.add(it); currentUid?.let { uid -> database.child("users").child(uid).child("medical_profile").child("allergies").setValue(allergies.toList()) } } },
                onAllergyRemove = { allergies.remove(it); if(allergies.isEmpty()) allergies.add("None"); currentUid?.let { uid -> database.child("users").child(uid).child("medical_profile").child("allergies").setValue(allergies.toList()) } },
                onConditionAdd    = { if (!conditions.contains(it)) { conditions.add(it); currentUid?.let { uid -> database.child("users").child(uid).child("medical_profile").child("conditions").setValue(conditions.toList()) } } },
                onConditionRemove = { conditions.remove(it); currentUid?.let { uid -> database.child("users").child(uid).child("medical_profile").child("conditions").setValue(conditions.toList()) } },
                onCheckupSave   = { date, clinic -> lastCheckDate = date; lastCheckClinic = clinic; currentUid?.let { uid -> database.child("users").child(uid).child("medical_profile").child("last_check_date").setValue(date); database.child("users").child(uid).child("medical_profile").child("last_check_clinic").setValue(clinic) } },
                onMedicineSave = { name, dosage, type, isPrn, slots, sDate, eDate ->
                    if (editingMedName != null) {
                        medicines.removeAll { it.name == editingMedName }
                    }
                    val startId = (medicines.maxOfOrNull { it.id } ?: 0) + 1
                    val newMeds = slots.mapIndexed { index, slot ->
                        Medicine(id = startId + index, name = name, dosage = dosage, type = type, isPrn = isPrn, slot = slot, startDate = sDate, endDate = eDate)
                    }
                    medicines.addAll(newMeds)
                    currentUid?.let { database.child("users").child(it).child("medical_profile").child("medicines").setValue(medicines.toList()) }
                    editingMedName = null
                },
                onClose       = { editMode = MedEditMode.NONE; editingMedName = null }
            )
        }
    }
}

// ─────────────────────────────────────────────────
//  MEDICATION TIME BLOCK
// ─────────────────────────────────────────────────

@Composable
private fun MedicationTimeBlock(
    slotLabel  : String,
    slotKey    : String,
    medicines  : MutableList<Medicine>,
    takenMap   : MutableMap<Int, Boolean>,
    fontSize   : AppFontSize,
    icon       : ImageVector,
    modifier   : Modifier = Modifier,
    database   : DatabaseReference,
    currentUid : String?,
    onEditClick: (String) -> Unit
) {
    val slotMeds = medicines.filter { it.slot == slotKey }
    if (slotMeds.isEmpty()) return

    val allTaken = slotMeds.all { takenMap[it.id] == true }
    val cardBg by animateColorAsState(if (allTaken) ElderlyColors.HeartbeatGreen.copy(alpha = 0.12f) else ElderlyColors.Surface, tween(400))
    val borderColor by animateColorAsState(if (allTaken) ElderlyColors.HeartbeatGreen.copy(alpha = 0.5f) else ElderlyColors.GrayBorder, tween(400))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x10000000), spotColor = Color(0x10000000))
            .background(cardBg, RoundedCornerShape(20.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = icon, contentDescription = null, tint = if (allTaken) ElderlyColors.HeartbeatGreen else ElderlyColors.NavyBlue, modifier = Modifier.size(20.dp))
                    Text(slotLabel, fontSize = 16.scaled(fontSize), fontWeight = FontWeight.Bold, color = if (allTaken) ElderlyColors.HeartbeatGreen else ElderlyColors.PrimaryText)
                }
                AnimatedVisibility(visible = allTaken) {
                    Box(modifier = Modifier.background(ElderlyColors.HeartbeatGreen, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text("Taken", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            slotMeds.forEach { med ->
                val taken = takenMap[med.id] == true
                val rowBg by animateColorAsState(if (taken) ElderlyColors.HeartbeatGreen else ElderlyColors.GrayLight, tween(300))
                val textColor = if (taken) Color.White else ElderlyColors.PrimaryText

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(rowBg).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newState = !taken
                        takenMap[med.id] = newState
                        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

                        currentUid?.let { uid ->
                            val logRef = database.child("users").child(uid).child("medical_profile").child("taken_logs").child(todayStr).child(med.id.toString())
                            if (newState) logRef.setValue(true) else logRef.removeValue()
                        }
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(if (taken) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null, tint = if (taken) Color.White else ElderlyColors.GrayMuted)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(getIconForType(med.type), null, tint = ElderlyColors.NavyBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(med.name, fontSize = 15.scaled(fontSize), fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("${med.dosage} ${getUnitForType(med.type)}", fontSize = 13.scaled(fontSize), color = if (taken) Color.White.copy(0.8f) else ElderlyColors.GrayMuted)
                        if (med.endDate.isNotBlank()) {
                            Text("Until: ${med.endDate}", fontSize = 11.scaled(fontSize), color = if (taken) Color.White.copy(0.7f) else ElderlyColors.Crimson)
                        }
                    }

                    if (!taken) {
                        IconButton(onClick = { onEditClick(med.name) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Edit, "Edit", tint = ElderlyColors.GrayMuted, modifier = Modifier.size(18.dp))
                        }

                        IconButton(onClick = {
                            medicines.removeAll { it.id == med.id }
                            currentUid?.let { database.child("users").child(it).child("medical_profile").child("medicines").setValue(medicines.toList()) }
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Delete, "Delete", tint = ElderlyColors.Crimson, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Text("Taken", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  MED EDIT WORKSPACE (COMPOSABLE TAG LOADED FIX)
// ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable // 🟢 FIX 1: Tag added successfully!
private fun MedEditWorkspace(
    mode                 : MedEditMode,
    editingMedName       : String?,
    allMeds              : List<Medicine>,
    bloodType            : String,
    allergies            : List<String>,
    conditions           : List<String>,
    lastCheckDate        : String,
    lastCheckClinic      : String,
    onBloodTypeChange    : (String) -> Unit,
    onAllergyAdd         : (String) -> Unit,
    onAllergyRemove      : (String) -> Unit,
    onConditionAdd       : (String) -> Unit,
    onConditionRemove    : (String) -> Unit,
    onCheckupSave        : (String, String) -> Unit,
    onMedicineSave       : (String, String, String, Boolean, List<String>, String, String) -> Unit,
    onClose              : () -> Unit
) {
    // 🟢 FIX 2: State variables fully declared here before UI triggers!
    var newMedName       by remember { mutableStateOf("") }
    var newMedDosage     by remember { mutableStateOf("") }
    var newMedType       by remember { mutableStateOf("Pill") }
    var isPrn            by remember { mutableStateOf(false) }
    val newMedSlots      = remember { mutableStateListOf<String>() }
    var customAllergy    by remember { mutableStateOf("") }
    var customCondition  by remember { mutableStateOf("") }

    var checkDateInput   by remember { mutableStateOf(lastCheckDate) }
    var checkClinicInput by remember { mutableStateOf(lastCheckClinic) }

    var startDate by remember { mutableStateOf("") }
    var endDate   by remember { mutableStateOf("") }
    var isOngoing by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val dateFormatter = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }

    LaunchedEffect(editingMedName) {
        if (editingMedName != null) {
            val matchingMeds = allMeds.filter { it.name == editingMedName }
            if (matchingMeds.isNotEmpty()) {
                val first = matchingMeds.first()
                newMedName = first.name
                newMedDosage = first.dosage
                newMedType = first.type
                isPrn = first.isPrn
                startDate = first.startDate
                endDate = first.endDate
                isOngoing = first.endDate.isBlank()

                newMedSlots.clear()
                newMedSlots.addAll(matchingMeds.map { it.slot })
            }
        } else {
            newMedName = ""
            newMedDosage = ""
            newMedType = "Pill"
            isPrn = false
            startDate = ""
            endDate = ""
            isOngoing = false
            newMedSlots.clear()
        }
    }

    // Dynamic reset when switching checkup target entries
    LaunchedEffect(lastCheckDate, lastCheckClinic) {
        checkDateInput = lastCheckDate
        checkClinicInput = lastCheckClinic
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    startDate = if (startMillis != null) dateFormatter.format(java.util.Date(startMillis)) else ""
                    endDate = if (endMillis != null) dateFormatter.format(java.util.Date(endMillis)) else ""
                    isOngoing = endDate.isBlank()
                }) {
                    Text("Save Dates", color = ElderlyColors.NavyBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = ElderlyColors.GrayMuted) }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                title = { Text("Select Duration", modifier = Modifier.padding(16.dp)) },
                headline = { Text("Start Date - End Date", modifier = Modifier.padding(horizontal = 16.dp)) },
                showModeToggle = false
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.45f)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .align(Alignment.BottomCenter)
                .background(ElderlyColors.Background, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Box(Modifier.width(44.dp).height(5.dp).background(ElderlyColors.GrayBorder, RoundedCornerShape(3.dp)).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    text = when(mode) {
                        MedEditMode.MEDICINE   -> if (editingMedName != null) "Edit Medication" else "Add Medication"
                        MedEditMode.BLOOD_TYPE -> "Select Blood Type"
                        MedEditMode.ALLERGIES  -> "Update Allergies"
                        MedEditMode.CONDITIONS -> "Update Medical Conditions"
                        MedEditMode.CHECKUP    -> "Update Last Check-up"
                        else                   -> ""
                    },
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ElderlyColors.NavyBlue
                )
                IconButton(onClick = onClose) {
                    Box(Modifier.size(38.dp).background(ElderlyColors.GrayLight, CircleShape), Alignment.Center) {
                        Icon(Icons.Filled.Close, "Close", tint = ElderlyColors.SecondaryText, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (mode) {
                    MedEditMode.MEDICINE -> {
                        WorkspaceLabel("Medication Name")
                        OutlinedTextField(value = newMedName, onValueChange = { newMedName = it }, placeholder = { Text("e.g., Uphamol") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = editFieldColors())
                        Spacer(Modifier.height(14.dp))

                        WorkspaceLabel("MEDICATION TYPE")
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Pill", "Capsule", "Liquid", "Injection", "Inhaler", "Drops", "Ointment", "Patch").forEach { medType ->
                                val isSel = newMedType == medType
                                Box(
                                    modifier = Modifier
                                        .background(if (isSel) ElderlyColors.NavyBlue else ElderlyColors.Surface, RoundedCornerShape(12.dp))
                                        .border(1.dp, if (isSel) ElderlyColors.NavyBlue else ElderlyColors.GrayBorder, RoundedCornerShape(12.dp))
                                        .clickable { newMedType = medType }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(medType, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = if (isSel) Color.White else ElderlyColors.SecondaryText)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))

                        WorkspaceLabel("Amount to Take")
                        val dynamicUnit = getUnitForType(newMedType)
                        OutlinedTextField(
                            value = newMedDosage,
                            onValueChange = { newMedDosage = it },
                            placeholder = { Text("e.g., 2") },
                            trailingIcon = { Text(dynamicUnit, modifier = Modifier.padding(end = 14.dp), color = ElderlyColors.GrayMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = editFieldColors()
                        )
                        Spacer(Modifier.height(14.dp))

                        WorkspaceLabel("Medication Duration (Optional)")
                        Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = startDate, onValueChange = {}, readOnly = true, enabled = false,
                                        placeholder = { Text("Start Date", color = ElderlyColors.GrayMuted) },
                                        trailingIcon = { Icon(Icons.Filled.DateRange, null, tint = ElderlyColors.NavyBlue) },
                                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = editFieldColors()
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = endDate, onValueChange = {}, readOnly = true, enabled = false,
                                        placeholder = { Text("End Date", color = ElderlyColors.GrayMuted) },
                                        trailingIcon = { Icon(Icons.Filled.EventBusy, null, tint = ElderlyColors.NavyBlue) },
                                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = editFieldColors()
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                isOngoing = !isOngoing
                                if (isOngoing) endDate = ""
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isOngoing, onCheckedChange = { isOngoing = it; if (it) endDate = "" }, colors = CheckboxDefaults.colors(checkedColor = ElderlyColors.NavyBlue))
                            Text("Ongoing Medication (No End Date)", fontSize = 14.sp, color = ElderlyColors.PrimaryText, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(20.dp))

                        AnimatedVisibility(visible = !isPrn) {
                            Column {
                                WorkspaceLabel("INTAKE TIMING — Choose one or more")
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        Triple("morning", "Morning", Icons.Filled.WbSunny),
                                        Triple("afternoon", "Afternoon", Icons.Filled.WbCloudy),
                                        Triple("night", "Night", Icons.Filled.NightsStay)
                                    ).forEach { (key, label, vectorIcon) ->
                                        val sel = newMedSlots.contains(key)
                                        val tintColor = if (sel) ElderlyColors.NavyBlue else ElderlyColors.GrayMuted
                                        Box(
                                            modifier = Modifier.weight(1f).height(74.dp)
                                                .border(2.dp, if (sel) ElderlyColors.NavyBlue else ElderlyColors.GrayBorder, RoundedCornerShape(14.dp))
                                                .background(if (sel) ElderlyColors.NavySurface else ElderlyColors.Surface, RoundedCornerShape(14.dp))
                                                .clickable { if (sel) newMedSlots.remove(key) else newMedSlots.add(key) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                Icon(imageVector = vectorIcon, contentDescription = null, tint = tintColor, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.height(4.dp))
                                                Text(label, fontSize = 13.sp, fontWeight = if(sel) FontWeight.ExtraBold else FontWeight.Normal, color = tintColor, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { isPrn = !isPrn }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isPrn, onCheckedChange = { isPrn = it }, colors = CheckboxDefaults.colors(checkedColor = ElderlyColors.NavyBlue))
                            Text("Take As Needed (PRN)", fontSize = 14.sp, color = ElderlyColors.PrimaryText, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(24.dp))

                        val isFrequencyBased = newMedType == "Ointment" || newMedType == "Cream" || newMedType == "Patch"
                        val typedAmount = newMedDosage.toIntOrNull() ?: 0

                        val isValid = when {
                            newMedName.isBlank() || newMedDosage.isBlank() -> false
                            isPrn -> true
                            newMedSlots.isEmpty() -> false
                            isFrequencyBased -> newMedSlots.size == typedAmount && typedAmount > 0
                            else -> true
                        }

                        if (!isPrn && isFrequencyBased && typedAmount > 0 && newMedSlots.size != typedAmount) {
                            Text("Please select exactly $typedAmount time slots for this application.", color = ElderlyColors.Crimson, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }

                        SaveButton(label = if (editingMedName != null) "Update Medication" else "Save Medication", enabled = isValid) {
                            if (isValid) {
                                val finalSlots = if (isPrn) listOf("prn") else newMedSlots.toList()
                                onMedicineSave(newMedName, newMedDosage, newMedType, isPrn, finalSlots, startDate, endDate)
                                onClose()
                            }
                        }
                    }

                    MedEditMode.BLOOD_TYPE -> {
                        WorkspaceLabel("SELECT YOUR BLOOD TYPE")
                        Spacer(Modifier.height(12.dp))
                        val rows = BloodTypes.chunked(4)
                        rows.forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), Arrangement.spacedBy(10.dp)) {
                                row.forEach { bt ->
                                    val sel = bt == bloodType
                                    Box(
                                        modifier = Modifier.weight(1f).height(64.dp)
                                            .border(2.5.dp, if (sel) ElderlyColors.Crimson else ElderlyColors.GrayBorder, RoundedCornerShape(14.dp))
                                            .background(if (sel) ElderlyColors.CrimsonLight else ElderlyColors.Surface, RoundedCornerShape(14.dp))
                                            .clickable { onBloodTypeChange(bt); onClose() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(bt, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (sel) ElderlyColors.Crimson else ElderlyColors.SecondaryText)
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }

                    MedEditMode.ALLERGIES -> {
                        WorkspaceLabel("COMMON ALLERGIES — Tap to toggle selection")
                        Spacer(Modifier.height(10.dp))
                        QuickAddChipGrid(items = QuickAllergies, selected = allergies, onAdd = onAllergyAdd, onRemove = onAllergyRemove)
                        Spacer(Modifier.height(16.dp))
                        WorkspaceLabel("OR TYPE MANUALLY")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = customAllergy, onValueChange = { customAllergy = it }, placeholder = { Text("Enter custom allergy...") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = editFieldColors())
                            Box(Modifier.size(56.dp).align(Alignment.CenterVertically).background(ElderlyColors.NavyBlue, RoundedCornerShape(14.dp)).clickable { if (customAllergy.isNotBlank()) { onAllergyAdd(customAllergy); customAllergy = "" } }, Alignment.Center) {
                                Icon(Icons.Filled.Add, "Add", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        SaveButton("Save Allergies") { onClose() }
                    }

                    MedEditMode.CONDITIONS -> {
                        WorkspaceLabel("COMMON CONDITIONS — Tap to toggle selection")
                        Spacer(Modifier.height(10.dp))
                        QuickAddChipGrid(items = QuickConditions, selected = conditions, onAdd = onConditionAdd, onRemove = onConditionRemove)
                        Spacer(Modifier.height(16.dp))
                        WorkspaceLabel("OR TYPE MANUALLY")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = customCondition, onValueChange = { customCondition = it }, placeholder = { Text("Enter custom condition...") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = editFieldColors())
                            Box(Modifier.size(56.dp).align(Alignment.CenterVertically).background(ElderlyColors.NavyBlue, RoundedCornerShape(14.dp)).clickable { if (customCondition.isNotBlank()) { onConditionAdd(customCondition); customCondition = "" } }, Alignment.Center) {
                                Icon(Icons.Filled.Add, "Add", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        SaveButton("Save Conditions") { onClose() }
                    }

                    MedEditMode.CHECKUP -> {
                        // 🟢 FIX 3: Fully wired native calendar wrapper with custom grey placeholder look!
                        val context = LocalContext.current
                        val calendar = Calendar.getInstance()

                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                                checkDateInput = formattedDate
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )

                        WorkspaceLabel("LAST CHECK-UP DATE")
                        Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                            OutlinedTextField(
                                value = checkDateInput,
                                onValueChange = { },
                                readOnly = true,
                                enabled = false,
                                placeholder = {
                                    Text("Tap to select date", fontSize = 16.sp, color = ElderlyColors.GrayMuted)
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = "Select Date",
                                        tint = ElderlyColors.NavyBlue
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = ElderlyColors.NavyBlue),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = ElderlyColors.NavyBlue,
                                    disabledBorderColor = ElderlyColors.GrayBorder,
                                    disabledContainerColor = ElderlyColors.Surface,
                                    disabledPlaceholderColor = ElderlyColors.GrayMuted,
                                    disabledTrailingIconColor = ElderlyColors.NavyBlue
                                )
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        WorkspaceLabel("HOSPITAL / CLINIC NAME")
                        OutlinedTextField(
                            value = checkClinicInput,
                            onValueChange = { checkClinicInput = it },
                            placeholder = { Text("e.g., Klinik Kesihatan Durian Tunggal", color = ElderlyColors.GrayMuted) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = ElderlyColors.NavyBlue),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = editFieldColors()
                        )

                        Spacer(Modifier.height(24.dp))

                        SaveButton("Save Check-up Info", enabled = checkDateInput.isNotBlank()) {
                            onCheckupSave(checkDateInput, checkClinicInput)
                            onClose()
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  ELDERLY CARD CUSTOM REUSABLE CONTAINER
// ─────────────────────────────────────────────────
@Composable
private fun ElderlyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = ElderlyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content   = content
    )
}

// ─────────────────────────────────────────────────
//  HELPER COMPOSABLES
// ─────────────────────────────────────────────────

@Composable
private fun MedSectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.GrayMuted, letterSpacing = 1.4.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
}

@Composable
private fun WorkspaceLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElderlyColors.GrayMuted, letterSpacing = 1.2.sp, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun FlowChips(items: List<String>, chipColor: Color, chipBg: Color) {
    val rows = items.chunked(3)
    rows.forEach { row ->
        Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { item ->
                Box(Modifier.background(chipBg, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(item, fontSize = 13.sp, color = chipColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun QuickAddChipGrid(
    items    : List<String>,
    selected : List<String>,
    onAdd    : (String) -> Unit,
    onRemove : (String) -> Unit
) {
    val rows = items.chunked(2)
    rows.forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(8.dp)) {
            row.forEach { item ->
                val sel = selected.contains(item)
                Box(
                    modifier = Modifier.weight(1f)
                        .background(if (sel) ElderlyColors.NavyBlue else ElderlyColors.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, if (sel) ElderlyColors.NavyBlue else ElderlyColors.GrayBorder, RoundedCornerShape(12.dp))
                        .clickable { if (sel) onRemove(item) else onAdd(item) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(item, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel) Color.White else ElderlyColors.SecondaryText, lineHeight = 17.sp)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SaveButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (enabled) {
        Brush.horizontalGradient(listOf(ElderlyColors.NavyBlue, Color(0xFF2D5288)))
    } else {
        Brush.horizontalGradient(listOf(ElderlyColors.GrayBorder, ElderlyColors.GrayMuted))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(bg, RoundedCornerShape(17.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = ElderlyColors.NavyBlue,
    unfocusedBorderColor = ElderlyColors.GrayBorder,
    cursorColor          = ElderlyColors.NavyBlue
)