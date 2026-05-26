import sys
print("STEP 1: Starting imports...")

import cv2
print("STEP 2: OpenCV imported successfully.")

import time
print("STEP 3: Time imported successfully.")

import mediapipe as mp
print("STEP 4: MediaPipe imported successfully.")

print("STEP 5: Initializing MediaPipe Pose model...")
mp_pose = mp.solutions.pose
pose = mp_pose.Pose(min_detection_confidence=0.5, min_tracking_confidence=0.5)

print("STEP 6: MediaPipe initialized. Attempting to connect to camera...")
# Trying to open the camera
cap = cv2.VideoCapture(0)

print("STEP 7: Camera connection command finished. Checking status...")
if not cap.isOpened():
    print("STEP 8: Camera is locked or busy, but Python DID NOT crash.")
else:
    print("STEP 8: SUCCESS! Camera is open and ready.")
    
print("STEP 9: Script finished safely.")