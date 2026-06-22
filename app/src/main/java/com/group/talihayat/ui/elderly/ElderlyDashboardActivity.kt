package com.group.talihayat.ui.elderly

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group.talihayat.service.FallDetectionService
import com.group.talihayat.service.MedicationReminderReceiver
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import java.util.Calendar

class ElderlyDashboardActivity : ComponentActivity() {

    private var isFallDetected = false
    private var countDownTimer: CountDownTimer? = null
    private lateinit var database: DatabaseReference
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private val stepsToday = mutableIntStateOf(0)

    private val uiState      = mutableStateOf(ElderlyUiState.MONITORING)
    private val countdown    = mutableIntStateOf(15)
    private val countdownDuration = mutableIntStateOf(15)
    private val fallSensitivity = mutableStateOf(1f)

    private val cloudSynced  = mutableStateOf(true)
    private val batteryLevel = mutableIntStateOf(100)
    private val cameraOnline = mutableStateOf(true)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                val currentBattery = (level * 100 / scale)
                batteryLevel.intValue = currentBattery

                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUid != null) {
                    database.child("users").child(currentUid).child("battery_level").setValue(currentBattery)
                }
            }
        }
    }

    private val fallEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.group.talihayat.FALL_DETECTED") {
                val emergencyIntent = Intent(this@ElderlyDashboardActivity, EmergencyAlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(emergencyIntent)
            }
        }
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                val totalSteps = event.values[0].toInt()
                val prefs = getSharedPreferences("TaliHayat_Steps", Context.MODE_PRIVATE)
                val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val savedDate = prefs.getString("last_date", "")

                if (savedDate != currentDate) {
                    prefs.edit().putString("last_date", currentDate).putInt("offset_steps", totalSteps).apply()
                }

                val offset = prefs.getInt("offset_steps", totalSteps)
                val currentDailySteps = totalSteps - offset
                stepsToday.intValue = currentDailySteps

                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    database.child("users").child(uid).child("steps_today").setValue(currentDailySteps)
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 100)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val callPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) != PackageManager.PERMISSION_GRANTED) {
            callPermissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            callPermissions.add(Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            callPermissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (callPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, callPermissions.toTypedArray(), 102)
        }

        scheduleDailyMedicationAlarms()

        val prefs = getSharedPreferences("TaliHayat_Prefs", Context.MODE_PRIVATE)
        fallSensitivity.value = prefs.getFloat("fall_sensitivity", 1f)

        val passedName = intent.getStringExtra("USER_NAME")
        if (passedName != null) {
            prefs.edit().putString("saved_user_name", passedName).apply()
        }
        val userName = passedName ?: prefs.getString("saved_user_name", "User") ?: "User"

        startPhoneHeartbeat()
        observeCameraStatus()
        observeCloudReachability()

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ContextCompat.registerReceiver(this, fallEventReceiver, IntentFilter("com.group.talihayat.FALL_DETECTED"), ContextCompat.RECEIVER_NOT_EXPORTED)

        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf("loading") }
                var isLinked by remember { mutableStateOf(false) }
                var isAlreadyPaired by remember { mutableStateOf(false) }
                var myPairingToken by remember { mutableStateOf(generateRandomPairingCode()) }
                var qrCountdown by remember { mutableStateOf(60) }
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                val caregiversList = remember { androidx.compose.runtime.mutableStateListOf<CaregiverData>() }

                LaunchedEffect(currentUid) {
                    if (currentUid != null) {
                        database.child("users").child(currentUid).child("caregivers")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    caregiversList.clear()
                                    if (snapshot.exists()) {
                                        isAlreadyPaired = true
                                        if (currentScreen == "pairing") {
                                            isLinked = true
                                        } else {
                                            currentScreen = "dashboard"
                                        }
                                        for (child in snapshot.children) {
                                            val caregiverId = child.key ?: continue
                                            database.child("users").child(caregiverId)
                                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                                    override fun onDataChange(cgSnapshot: DataSnapshot) {
                                                        val name = cgSnapshot.child("name").getValue(String::class.java) ?: "Caregiver"
                                                        val phone = cgSnapshot.child("phone").getValue(String::class.java) ?: ""
                                                        caregiversList.removeAll { it.id == caregiverId }
                                                        caregiversList.add(CaregiverData(caregiverId, name, phone))
                                                    }
                                                    override fun onCancelled(error: DatabaseError) {}
                                                })
                                        }
                                    } else {
                                        if (currentScreen != "pairing") {
                                            currentScreen = "pairing"
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    if (currentScreen != "pairing") {
                                        currentScreen = "pairing"
                                    }
                                }
                            })
                    } else {
                        if (currentScreen != "pairing") {
                            currentScreen = "pairing"
                        }
                    }
                }

                LaunchedEffect(currentScreen) {
                    if (currentScreen == "pairing") {
                        isLinked = false
                        myPairingToken = generateRandomPairingCode()
                    }
                }

                LaunchedEffect(myPairingToken, currentScreen) {
                    if (currentScreen != "pairing") return@LaunchedEffect
                    isLinked = false
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
                    val tokenRef = database.child("pairing_tokens").child(myPairingToken)
                    tokenRef.setValue(mapOf("elderlyUid" to uid, "status" to "waiting"))

                    val listener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.child("status").getValue(String::class.java) == "linked") {
                                isLinked = true
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    }
                    tokenRef.addValueEventListener(listener)

                    qrCountdown = 60
                    while (qrCountdown > 0 && currentScreen == "pairing" && !isLinked) { delay(1000L); qrCountdown-- }
                    tokenRef.removeEventListener(listener); tokenRef.removeValue()
                    if (currentScreen == "pairing" && !isLinked) myPairingToken = generateRandomPairingCode()
                }

                when (currentScreen) {
                    "loading" -> { Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1E3A5F)) } }
                    "pairing" -> {
                        PairingScreen(
                            pairingToken = myPairingToken,
                            countdown = qrCountdown,
                            isLinked = isLinked,
                            onBackClick = {
                                if (isAlreadyPaired) {
                                    currentScreen = "dashboard"
                                } else {
                                    finish()
                                }
                            },
                            onPairingSuccess = { currentScreen = "dashboard" }
                        )
                    }
                    "dashboard" -> {
                        ElderlyDashboardScreen(
                            userName          = userName,
                            caregivers        = caregiversList,
                            uiState           = uiState.value,
                            countdown         = countdown.intValue,
                            countdownDuration = countdownDuration.intValue,
                            fallSensitivity   = fallSensitivity.value,
                            batteryLevel      = batteryLevel.intValue,
                            cloudSynced       = cloudSynced.value,
                            cameraOnline      = cameraOnline.value,
                            stepsCount        = stepsToday.intValue,
                            onCancel          = { cancelFall() },
                            onSimulate        = {
                                 val emergencyIntent = Intent(this@ElderlyDashboardActivity, EmergencyAlertActivity::class.java).apply {
                                     flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                 }
                                 startActivity(emergencyIntent)
                             },
                            onCountdownChange = { countdownDuration.intValue = it },
                            onFallSensitivityChange = { updateHardwareSensitivity(it) },
                            onConnectCaregiver = { currentScreen = "pairing" },
                            onLogout = {
                                 cancelFall()
                                 FirebaseAuth.getInstance().signOut()
                                 val prefs = getSharedPreferences("TaliHayat_Prefs", MODE_PRIVATE)
                                 prefs.edit().remove("user_role").remove("saved_user_name").apply()

                                 // Explicitly stop the fall detection service on logout
                                 val serviceIntent = Intent(this@ElderlyDashboardActivity, com.group.talihayat.service.FallDetectionService::class.java)
                                 stopService(serviceIntent)

                                 val intent = Intent(this@ElderlyDashboardActivity, com.group.talihayat.ui.auth.AuthActivity::class.java).apply {
                                     flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                 }
                                 startActivity(intent)
                                 finish()
                             }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); stepSensor?.let { sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_UI) } }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(stepListener) }

    private fun generateRandomPairingCode(): String = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")

    private fun updateHardwareSensitivity(value: Float) {
        fallSensitivity.value = value
        getSharedPreferences("TaliHayat_Prefs", Context.MODE_PRIVATE).edit().putFloat("fall_sensitivity", value).apply()
        startForegroundService(Intent(this, FallDetectionService::class.java).apply { putExtra("sensitivity_value", value) })
    }

    private fun startPhoneHeartbeat() {
        heartbeatHandler.post(object : Runnable {
            override fun run() {
                database.child("heartbeat").child("phone_ts").setValue(System.currentTimeMillis() / 1000)
                heartbeatHandler.postDelayed(this, 5000)
            }
        })
    }

    private fun observeCameraStatus() {
        database.child("heartbeat").child("camera_ts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cameraTs  = snapshot.getValue(Long::class.java) ?: 0
                val online    = ((System.currentTimeMillis() / 1000) - cameraTs) <= 10
                cameraOnline.value = online
                if (!isFallDetected) uiState.value = if (online) ElderlyUiState.MONITORING else ElderlyUiState.CAMERA_OFFLINE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun observeCloudReachability() {
        FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { cloudSynced.value = snapshot.getValue(Boolean::class.java) ?: false }
            override fun onCancelled(error: DatabaseError) { cloudSynced.value = false }
        })
    }

    private fun triggerFallUI() {
        database.child("status").child("phone_detected").setValue(true)
        database.child("fall_history").push().setValue(mapOf("device" to "Smartphone (IMU)", "timestamp" to System.currentTimeMillis() / 1000, "event" to "Impact Detected"))

        // 🟢 NEW: Also push fall alerts to the visual bell!
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            val alertData = mapOf(
                "title" to "Fall Detected!",
                "description" to "A potential fall was detected by your phone.",
                "timeString" to java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                "timestamp" to System.currentTimeMillis(),
                "iconType" to "alert"
            )
            database.child("users").child(uid).child("notifications").push().setValue(alertData)
        }

        vibrator?.let { v -> if (v.hasVibrator()) v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)) }
        uiState.value = ElderlyUiState.FALL_COUNTDOWN
        countdown.intValue = countdownDuration.intValue

        countDownTimer = object : CountDownTimer(countdownDuration.intValue * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) { countdown.intValue = (millisUntilFinished / 1000).toInt() }
            override fun onFinish() { countdown.intValue = 0; uiState.value = ElderlyUiState.HELP_SENT; vibrator?.cancel(); database.child("status").child("phone_confirmed").setValue(true) }
        }.start()
    }

    private fun cancelFall() {
        countDownTimer?.cancel(); vibrator?.cancel(); isFallDetected = false
        database.child("status").child("phone_detected").setValue(false)
        countdown.intValue = countdownDuration.intValue
        uiState.value = if (cameraOnline.value) ElderlyUiState.MONITORING else ElderlyUiState.CAMERA_OFFLINE
    }

    private fun scheduleDailyMedicationAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 🟢 Morning: Hourly from 6:00 AM to 11:00 AM
        for (hour in 6..11) {
            scheduleHourlyAlarm(alarmManager, hour, "morning", 100 + hour, isFinal = (hour == 11))
        }

        // 🟢 Afternoon: Hourly from 12:00 PM to 5:00 PM
        for (hour in 12..17) {
            scheduleHourlyAlarm(alarmManager, hour, "afternoon", 200 + hour, isFinal = (hour == 17))
        }

        // 🟢 Night: Hourly from 6:00 PM to 10:00 PM
        for (hour in 18..22) {
            scheduleHourlyAlarm(alarmManager, hour, "night", 300 + hour, isFinal = (hour == 22))
        }
    }

    private fun scheduleHourlyAlarm(alarmManager: AlarmManager, hour: Int, slotName: String, intentId: Int, isFinal: Boolean) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val intent = Intent(this, MedicationReminderReceiver::class.java).apply {
            putExtra("SLOT_NAME", slotName)
            putExtra("NOTIFICATION_ID", intentId)
            putExtra("USER_UID", currentUid)
            putExtra("IS_FINAL", isFinal) // 🟢 Flags the very last hour of the window!
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, intentId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun scheduleAlarmForSlot(alarmManager: AlarmManager, hour: Int, minute: Int, slotName: String, intentId: Int) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val intent = Intent(this, MedicationReminderReceiver::class.java).apply {
            putExtra("SLOT_NAME", slotName)
            putExtra("NOTIFICATION_ID", intentId)
            putExtra("USER_UID", currentUid) // 🟢 MAGIC: Pass the user ID!
        }
        val pendingIntent = PendingIntent.getBroadcast(this, intentId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatHandler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(batteryReceiver); unregisterReceiver(fallEventReceiver) } catch (e: Exception) {}
        countDownTimer?.cancel()
    }
}

data class CaregiverData(val id: String, val name: String, val phone: String)