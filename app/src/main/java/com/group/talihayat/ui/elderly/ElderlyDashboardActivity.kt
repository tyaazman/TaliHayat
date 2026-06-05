package com.group.talihayat.ui.elderly

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group.talihayat.service.FallDetectionService

class ElderlyDashboardActivity : ComponentActivity() {

    private var isFallDetected = false
    private var countDownTimer: CountDownTimer? = null
    private lateinit var database: DatabaseReference
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null

    private val uiState      = mutableStateOf(ElderlyUiState.MONITORING)
    private val countdown    = mutableIntStateOf(15)
    private val cloudSynced  = mutableStateOf(true)
    private val batteryLevel = mutableIntStateOf(100)
    private val cameraOnline = mutableStateOf(true) // Track camera independently

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                batteryLevel.intValue = (level * 100 / scale)
            }
        }
    }

    private val fallEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("TaliHayat", "Broadcast Received: ${intent?.action}")
            if (intent?.action == "com.group.talihayat.FALL_DETECTED" && !isFallDetected) {
                Log.d("TaliHayat", "Triggering Fall UI now...")
                isFallDetected = true
                triggerFallUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = FirebaseDatabase.getInstance(
            "https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val serviceIntent = Intent(this, FallDetectionService::class.java)
        startForegroundService(serviceIntent)

        startPhoneHeartbeat()
        observeCameraStatus()
        observeCloudReachability()

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        ContextCompat.registerReceiver(
            this,
            fallEventReceiver,
            IntentFilter("com.group.talihayat.FALL_DETECTED"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            MaterialTheme {
                ElderlyDashboardScreen(
                    uiState = uiState.value,
                    countdown = countdown.intValue,
                    batteryLevel = batteryLevel.intValue,
                    cloudSynced = cloudSynced.value,
                    cameraOnline = cameraOnline.value, // Pass camera status
                    onCancel = { cancelFall() },
                    onSimulate = {
                        if (!isFallDetected) {
                            isFallDetected = true; triggerFallUI()
                        }
                    }
                )
            }
        }
    }

    private fun startPhoneHeartbeat() {
        heartbeatHandler.post(object : Runnable {
            override fun run() {
                val currentTs = System.currentTimeMillis() / 1000
                database.child("heartbeat").child("phone_ts").setValue(currentTs)
                heartbeatHandler.postDelayed(this, 5000)
            }
        })
    }

    private fun observeCameraStatus() {
        database.child("heartbeat").child("camera_ts")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cameraTs  = snapshot.getValue(Long::class.java) ?: 0
                    val currentTs = System.currentTimeMillis() / 1000
                    val online    = (currentTs - cameraTs) <= 10

                    cameraOnline.value = online // Update camera status independently

                    if (!isFallDetected) {
                        uiState.value = if (online) ElderlyUiState.MONITORING
                        else        ElderlyUiState.CAMERA_OFFLINE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun observeCloudReachability() {
        val connectedRef = FirebaseDatabase.getInstance(
            "https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cloudSynced.value = snapshot.getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {
                cloudSynced.value = false
            }
        })
    }

    private fun triggerFallUI() {
        database.child("status").child("phone_detected").setValue(true)

        val phoneLog = mapOf(
            "device"    to "Smartphone (IMU)",
            "timestamp" to System.currentTimeMillis() / 1000,
            "event"     to "Impact Detected"
        )
        database.child("fall_history").push().setValue(phoneLog)

        vibrator?.let { v ->
            if (v.hasVibrator()) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
            }
        }

        uiState.value   = ElderlyUiState.FALL_COUNTDOWN
        countdown.intValue = 15

        countDownTimer = object : CountDownTimer(15_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown.intValue = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                countdown.intValue = 0
                uiState.value   = ElderlyUiState.HELP_SENT
                vibrator?.cancel()
                database.child("status").child("phone_confirmed").setValue(true)
            }
        }.start()
    }

    private fun cancelFall() {
        countDownTimer?.cancel()
        vibrator?.cancel()
        isFallDetected = false
        database.child("status").child("phone_detected").setValue(false)
        countdown.intValue = 15
        uiState.value   = if (cameraOnline.value) ElderlyUiState.MONITORING else ElderlyUiState.CAMERA_OFFLINE
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatHandler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(fallEventReceiver)
        } catch (e: Exception) {}
        countDownTimer?.cancel()
    }
}