package com.group.talihayat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastAlertTime: Long = 0

    // 🟢 Dynamic threshold property that adapts to user preference settings
    private var impactThreshold = 3.0f

    companion object {
        private const val TAG = "FallDetectionService"
        private const val CHANNEL_ID = "TaliHayat_Background_Channel"
        private const val NOTIFICATION_ID = 99
        private const val ALERT_COOLDOWN = 5000L
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Load initial user choice configurations from persistent memory
        val prefs = getSharedPreferences("TaliHayat_Prefs", Context.MODE_PRIVATE)
        val savedSensitivity = prefs.getFloat("fall_sensitivity", 1f)
        updateThresholdValue(savedSensitivity)

        if (accelerometer == null) {
            Log.e(TAG, "No Accelerometer found!")
        } else {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Service Created. Active monitoring boundary initialized to: $impactThreshold G")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        // 🟢 Live update sensitivity threshold parameters when an adjustment is broadcasted from UI layers
        val sensitivityIntentValue = intent?.getFloatExtra("sensitivity_value", -1f) ?: -1f
        if (sensitivityIntentValue != -1f) {
            updateThresholdValue(sensitivityIntentValue)
            Log.d(TAG, "Live hardware sensor boundary updated to: $impactThreshold G")
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TaliHayat Safety Active")
            .setContentText("Monitoring for falls...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun updateThresholdValue(sensitivity: Float) {
        impactThreshold = when (sensitivity) {
            0f -> 6.0f   // 🟢 New Low Sensitivity: Requires a massive drop/hard impact (ignores almost all casual shakes)
            2f -> 1.5f   // 🔴 New High Sensitivity: Ultra-responsive, triggers on very light slips or shifts
            else -> 4.5f // 🔵 New Normal Sensitivity: Balanced baseline (what your old low setting used to be!)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val gForce = sqrt(
                (event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]).toDouble()
            ).toFloat() / 9.81f

            // 🟢 FIXED: Verify reading metrics dynamically against our adaptive impactThreshold state property
            if (gForce > impactThreshold) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAlertTime > ALERT_COOLDOWN) {
                    lastAlertTime = currentTime
                    Log.e(TAG, "IMPACT DETECTED: $gForce G (Threshold Boundary: $impactThreshold G)")

                    vibrate()

                    val intent = Intent("com.group.talihayat.FALL_DETECTED")
                    intent.`package` = packageName
                    sendBroadcast(intent)
                }
            }
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
        } else {
            @Suppress("DEPRECATION")
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Safety",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}