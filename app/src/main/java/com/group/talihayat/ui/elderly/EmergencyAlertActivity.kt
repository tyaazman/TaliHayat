package com.group.talihayat.ui.elderly

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmergencyAlertActivity : ComponentActivity() {

    private var countDownTimer: CountDownTimer? = null
    private var vibrator: Vibrator? = null
    private val database = FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bypass lock screen & wake the screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Trigger Fall actions in Firebase
        triggerFallActions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    var countdown by remember { mutableIntStateOf(15) }
                    var isHelpSent by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        vibrator?.let { v ->
                            if (v.hasVibrator()) {
                                // Vibrate continuously (500ms on, 250ms off) during countdown
                                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500), 0))
                            }
                        }

                        countDownTimer = object : CountDownTimer(15000L, 1000L) {
                            override fun onTick(millisUntilFinished: Long) {
                                countdown = (millisUntilFinished / 1000).toInt()
                            }

                            override fun onFinish() {
                                countdown = 0
                                isHelpSent = true
                                vibrator?.cancel()
                                confirmHelpSent()
                            }
                        }.start()
                    }

                    if (isHelpSent) {
                        HelpSentOverlay()
                        // Automatically close activity after 4 seconds of displaying help sent screen
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(4000L)
                            dismissSosNotification()
                            finish()
                        }
                    } else {
                        FallCountdownOverlay(
                            countdown = countdown,
                            onCancel = {
                                cancelFallAlert()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun triggerFallActions() {
        database.child("status").child("phone_detected").setValue(true)
        database.child("fall_history").push().setValue(
            mapOf(
                "device" to "Smartphone (IMU)",
                "timestamp" to System.currentTimeMillis() / 1000,
                "event" to "Impact Detected"
            )
        )

        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            val alertData = mapOf(
                "title" to "Fall Detected!",
                "description" to "A potential fall was detected by your phone.",
                "timeString" to SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                "timestamp" to System.currentTimeMillis(),
                "iconType" to "alert"
            )
            database.child("users").child(uid).child("notifications").push().setValue(alertData)
        }
    }

    private fun confirmHelpSent() {
        database.child("status").child("phone_confirmed").setValue(true)
    }

    private fun cancelFallAlert() {
        countDownTimer?.cancel()
        vibrator?.cancel()
        database.child("status").child("phone_detected").setValue(false)
        dismissSosNotification()
        finish()
    }

    private fun dismissSosNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(100)
        } catch (e: Exception) {
            // Ignore if notification cannot be cancelled
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        vibrator?.cancel()
    }
}
