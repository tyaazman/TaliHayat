package com.group.talihayat

import android.app.*
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastAlertTime: Long = 0

    companion object {
        private const val TAG = "FallDetectionService"
        private const val CHANNEL_ID = "TaliHayat_Background_Channel"
        private const val NOTIFICATION_ID = 99
        private const val IMPACT_THRESHOLD = 2.5f // Realistic impact for a drop
        private const val ALERT_COOLDOWN = 5000L 
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.e(TAG, "No Accelerometer found!")
        } else {
            // SENSOR_DELAY_GAME is better for battery while still being fast
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Service Created. Monitoring for impacts > $IMPACT_THRESHOLD G")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TaliHayat Safety Active")
            .setContentText("Monitoring for falls...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val gForce = sqrt(
                (event.values[0] * event.values[0] +
                 event.values[1] * event.values[1] +
                 event.values[2] * event.values[2]).toDouble()
            ).toFloat() / 9.81f

            if (gForce > IMPACT_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAlertTime > ALERT_COOLDOWN) {
                    lastAlertTime = currentTime
                    Log.e(TAG, "IMPACT DETECTED: $gForce G")
                    
                    // Direct haptic feedback so you know the sensor worked
                    vibrate()
                    
                    // Alert the Activity
                    val intent = Intent("com.group.talihayat.FALL_DETECTED")
                    intent.`package` = packageName
                    sendBroadcast(intent)
                }
            }
        }
    }

    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        // Double pulse
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
        v.vibrate(effect)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Safety", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}
