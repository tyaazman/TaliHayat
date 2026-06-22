package com.group.talihayat.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.CallLog
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EmergencyCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "EmergencyCallReceiver"
        const val EMERGENCY_NUMBER = "0192911487"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive: action = ${intent.action}")
        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val dialedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            Log.i(TAG, "Outgoing call intercepted via ACTION_NEW_OUTGOING_CALL: $dialedNumber")
            if (dialedNumber != null) {
                val isMatch = PhoneNumberUtils.compare(dialedNumber, EMERGENCY_NUMBER) || 
                              dialedNumber.replace(Regex("[^0-9]"), "").endsWith(EMERGENCY_NUMBER.replace(Regex("[^0-9]"), ""))
                if (isMatch) {
                    val currentTime = System.currentTimeMillis()
                    val prefs = context.getSharedPreferences("EmergencyCallReceiver_Prefs", Context.MODE_PRIVATE)
                    val lastProcessed = prefs.getLong("last_processed_call_date", 0L)
                    if (Math.abs(currentTime - lastProcessed) > 10000) {
                        prefs.edit().putLong("last_processed_call_date", currentTime).apply()
                        Log.w(TAG, "EMERGENCY CALL DETECTED via ACTION_NEW_OUTGOING_CALL")
                        val pendingResult = goAsync()
                        Thread {
                            try {
                                triggerFirebaseAlert()
                            } finally {
                                pendingResult.finish()
                            }
                        }.start()
                    }
                }
            }
        } else if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.i(TAG, "Phone state changed: $state")
            if (state == TelephonyManager.EXTRA_STATE_OFFHOOK || state == TelephonyManager.EXTRA_STATE_IDLE) {
                val pendingResult = goAsync()
                Thread {
                    try {
                        // Wait 1.5 seconds for Telecom to write the call log
                        Thread.sleep(1500)
                        checkLastOutgoingCall(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in background call check: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }.start()
            }
        }
    }

    private fun checkLastOutgoingCall(context: Context) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )
        val selection = "${CallLog.Calls.TYPE} = ?"
        val selectionArgs = arrayOf(CallLog.Calls.OUTGOING_TYPE.toString())
        val sortOrder = "${CallLog.Calls.DATE} DESC"

        var cursor: Cursor? = null
        try {
            cursor = resolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            if (cursor != null && cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                if (numberIndex != -1 && dateIndex != -1) {
                    val number = cursor.getString(numberIndex)
                    val date = cursor.getLong(dateIndex)
                    val timeDiff = System.currentTimeMillis() - date
                    
                    Log.i(TAG, "Last outgoing call log entry: number=$number, date=$date, diff=$timeDiff ms")
                    
                    if (number != null) {
                        val isMatch = PhoneNumberUtils.compare(number, EMERGENCY_NUMBER) || 
                                      number.replace(Regex("[^0-9]"), "").endsWith(EMERGENCY_NUMBER.replace(Regex("[^0-9]"), ""))
                        // Ensure the call was made recently (within last 30 seconds)
                        if (isMatch && timeDiff < 30000) {
                            val prefs = context.getSharedPreferences("EmergencyCallReceiver_Prefs", Context.MODE_PRIVATE)
                            val lastProcessed = prefs.getLong("last_processed_call_date", 0L)
                            if (date > lastProcessed) {
                                prefs.edit().putLong("last_processed_call_date", date).apply()
                                Log.w(TAG, "EMERGENCY CALL DETECTED via CallLog query to $EMERGENCY_NUMBER")
                                triggerFirebaseAlert()
                            } else {
                                Log.i(TAG, "Call log entry already processed for date: $date")
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while querying CallLog: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error querying CallLog: ${e.message}", e)
        } finally {
            cursor?.close()
        }
    }

    private fun triggerFirebaseAlert() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            Log.e(TAG, "No authenticated user found. Cannot log emergency call activity.")
            return
        }

        val database = FirebaseDatabase.getInstance("https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
        
        try {
            // Fetch user profile to check role and pairedElderlyUid
            val profileTask = database.child("users").child(currentUid).get()
            val snapshot = Tasks.await(profileTask)
            val role = snapshot.child("role").getValue(String::class.java) ?: "Elderly"
            val targetUid = if (role.trim().equals("Caretaker", ignoreCase = true)) {
                val paired = snapshot.child("pairedElderlyUid").getValue(String::class.java)
                Log.i(TAG, "Current logged-in user is Caretaker. Redirecting alert to paired Elderly UID: $paired")
                paired
            } else {
                currentUid
            }

            if (targetUid != null) {
                val alertId = database.child("users").child(targetUid).child("activities").push().key
                if (alertId != null) {
                    val alertData = mapOf(
                        "title" to "⚠️ Emergency Call Detected",
                        "eventType" to "EMERGENCY_CALL",
                        "timestamp" to System.currentTimeMillis()
                    )
                    val pushTask = database.child("users")
                        .child(targetUid)
                        .child("activities")
                        .child(alertId)
                        .setValue(alertData)
                    Tasks.await(pushTask)
                    Log.i(TAG, "Emergency activity log pushed successfully to target UID: $targetUid")
                }
            } else {
                Log.e(TAG, "Target UID for emergency call is null (Caretaker has no paired Elderly).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve target UID or push emergency activity log: ${e.message}", e)
        }
    }
}
