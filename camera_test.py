print("Starting TaliHayat: Research-Aligned Version with Heartbeat & Telegram...")
import cv2
import mediapipe as mp
import math
import time
import sys
import collections
import pyrebase 
import requests

# --- 1. Telegram Details ---
TELEGRAM_TOKEN = "8969715762:AAEgMpjouQ9sC-FWMZeTeRPblgiQorrMd6k"
CHAT_ID = "806779283"

def send_telegram_alert(message):
    url = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendMessage?chat_id={CHAT_ID}&text={message}"
    try:
        requests.get(url)
    except Exception as e:
        print(f"Telegram Error: {e}")

# --- 2. Firebase Configuration ---
config = {
    "apiKey": "AIzaSyCR5hHevNv6u5NeTg1oxvIU9Titr-oE9Y4",
    "authDomain": "talihayat-bfc99.firebaseapp.com",
    "databaseURL": "https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/",
    "projectId": "talihayat-bfc99",
    "storageBucket": "talihayat-bfc99.firebasestorage.app",
    "messagingSenderId": "520534760598",
    "appId": "1:520534760598:web:c3b347d45f4b05610236b7"
}

firebase = pyrebase.initialize_app(config)
db = firebase.database()

# --- 3. Academic Configuration ---
FALL_VELOCITY_THRESHOLD = 0.15 
CRITICAL_PHASE_DURATION = 5.0 

mp_pose = mp.solutions.pose
pose = mp_pose.Pose(min_detection_confidence=0.7, min_tracking_confidence=0.7)
mp_draw = mp.solutions.drawing_utils

y_history = collections.deque(maxlen=10)
fall_detected_time = None
status_text = "SYSTEM ACTIVE"
status_color = (0, 255, 0)

# Trackers
last_cloud_update = False 
last_heartbeat_send = 0 
telegram_sent = False 

cap = cv2.VideoCapture(1) # Use 0 for built-in, 1 for external
if not cap.isOpened():
    sys.exit("Error: Camera not found.")

print("TaliHayat is warming up...")
time.sleep(2)


while cap.isOpened():
    ret, frame = cap.read()
    if not ret: continue
    h, w, _ = frame.shape
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = pose.process(rgb)

    current_camera_detected = False 
    current_time = time.time()

    # --- 4. Heartbeat Logic (Every 2 seconds) ---
    if current_time - last_heartbeat_send > 2.0:
        try:
            db.child("heartbeat").update({"camera_ts": int(current_time)})
            last_heartbeat_send = current_time
        except: pass

    # --- 5. AI Posture Analysis ---
    if results.pose_landmarks:
        lm = results.pose_landmarks.landmark
        
        # Bounding Box Logic
        x_coords = [l.x for l in lm]
        y_coords = [l.y for l in lm]
        box_w = (max(x_coords) - min(x_coords)) * w
        box_h = (max(y_coords) - min(y_coords)) * h
        aspect_ratio = box_w / box_h 

        # Velocity Logic
        sh_y = (lm[mp_pose.PoseLandmark.LEFT_SHOULDER].y + lm[mp_pose.PoseLandmark.RIGHT_SHOULDER].y) / 2
        velocity = sh_y - y_history[0] if len(y_history) else 0
        y_history.append(sh_y)

        # Decision Logic
        if aspect_ratio > 1.0 and velocity > FALL_VELOCITY_THRESHOLD:
            if fall_detected_time is None: fall_detected_time = current_time
            status_text = "SUDDEN IMPACT DETECTED"
            status_color = (0, 165, 255) 
        elif aspect_ratio > 1.2: 
            if fall_detected_time and (current_time - fall_detected_time > CRITICAL_PHASE_DURATION):
                status_text = "CRITICAL FALL ALERT!"
                status_color = (0, 0, 255) 
                current_camera_detected = True 
            else:
                status_text = "Horizontal / Resting"
                status_color = (0, 255, 255) 
        else:
            status_text = "Normal (Upright)"
            status_color = (0, 255, 0) 
            fall_detected_time = None
            telegram_sent = False 

        # --- 6. Cloud Fusion & Telegram Logic ---
        try:
            db_status = db.child("status").get().val() or {}
            phone_impact = db_status.get("phone_detected", False)
            phone_sos = db_status.get("phone_confirmed", False)

            # Handle SOS from Phone (Countdown finished)
            if phone_sos == True:
                send_telegram_alert("🚨 EMERGENCY: Phone user detected a fall and DID NOT cancel!")
                db.child("status").update({"phone_confirmed": False, "phone_detected": False})
                telegram_sent = True 

            # Handle Real-time Sync & Fusion
            if (phone_impact or current_camera_detected) and not telegram_sent:
                if phone_impact and current_camera_detected:
                    send_telegram_alert("🚨 RED ALERT: High Confidence Fall (Camera + Phone)!")
                else:
                    send_telegram_alert("⚠️ YELLOW ALERT: Possible fall detected (Single Sensor).")
                telegram_sent = True

                # Log to History
                db.child("fall_history").push({
                    "timestamp": int(time.time()),
                    "alert": "RED" if (phone_impact and current_camera_detected) else "YELLOW"
                })

            if not phone_impact and not current_camera_detected:
                telegram_sent = False
        except Exception as e:
            print(f"DB Sync Error: {e}")

        # --- 7. Visual Overlays ---
        # Draw Skeleton
        mp_draw.draw_landmarks(frame, results.pose_landmarks, mp_pose.POSE_CONNECTIONS)
        
        # Header Background
        cv2.rectangle(frame, (0,0), (w, 60), (30,30,30), -1)
        
        # UI Text
        cv2.putText(frame, "TaliHayat Elderly Monitoring", (20, 40), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
        cv2.putText(frame, status_text, (20, 110), 
                    cv2.FONT_HERSHEY_SIMPLEX, 1.2, status_color, 3)
        cv2.putText(frame, f"AR: {round(aspect_ratio, 2)} | Vel: {round(velocity, 3)}", 
                    (20, h-20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)

    # Display Window
    cv2.imshow("TaliHayat Project - UTeM PSM", frame)
    if cv2.waitKey(1) & 0xFF == 27: break

cap.release()
cv2.destroyAllWindows()