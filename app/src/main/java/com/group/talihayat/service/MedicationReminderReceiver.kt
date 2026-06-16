package com.group.talihayat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group.talihayat.ui.elderly.ElderlyDashboardActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotName = intent.getStringExtra("SLOT_NAME") ?: "morning"
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 100)
        val uid = intent.getStringExtra("USER_UID") ?: return
        val isFinal = intent.getBooleanExtra("IS_FINAL", false)

        // 🟢 Pause the alarm system to check Firebase before deciding to notify!
        val pendingResult = goAsync()
        val db = FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.child("users").child(uid).child("medical_profile").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // 1. Get all active meds for this specific time slot
                    val allMeds = snapshot.child("medicines").children.mapNotNull { it.getValue(MedicineSnapshot::class.java) }
                    val slotMeds = allMeds.filter { it.slot == slotName }

                    if (slotMeds.isEmpty()) return // If they have 0 meds assigned to this slot, do nothing!

                    // 2. Check how many of those meds were checked off today
                    val takenLog = snapshot.child("taken_logs").child(todayStr)
                    val takenCount = slotMeds.count { takenLog.child(it.id.toString()).getValue(Boolean::class.java) == true }

                    val remaining = slotMeds.size - takenCount

                    // 3. If there are still meds to take, send the notification!
                    if (remaining > 0) {
                        val title = if (isFinal) "FINAL WARNING: Missed Medication" else "Hourly Medication Reminder"
                        val desc = if (isFinal) {
                            "The $slotName window is closing! You still have $remaining untaken medication(s)."
                        } else {
                            "You have $remaining untaken medication(s) for the $slotName. Tap to view."
                        }

                        // Push to Firebase Bell
                        val notifData = mapOf(
                            "title" to title,
                            "description" to desc,
                            "timeString" to SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                            "timestamp" to System.currentTimeMillis(),
                            "iconType" to if (isFinal) "alert" else "medication"
                        )
                        db.child("users").child(uid).child("notifications").push().setValue(notifData)

                        // Trigger Phone Notification
                        triggerAndroidNotification(context, title, desc, notificationId)
                    }
                } finally {
                    pendingResult.finish() // Tells Android we are done checking Firebase
                }
            }
            override fun onCancelled(error: DatabaseError) { pendingResult.finish() }
        })
    }

    private fun triggerAndroidNotification(context: Context, title: String, desc: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "talihayat_med_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Medication Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, ElderlyDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}

// Helper data class for parsing snapshot
private data class MedicineSnapshot(val id: Int = 0, val slot: String = "")