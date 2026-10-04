XRHUDAPP

Small AOSP-compatible Android app that reads the phone's GPS location and speed and sends it over UDP to the XRHUD host.

Designed to work with the XRHUD Python HUD application.

How it works
Android Phone GPS
       │
       │ UDP / JSON
       ▼
XRHUD Host :8676
       │
       ▼
hud_app.py
       │
       ├── Speed
       └── Location


The phone sends GPS data directly to the configured target IP on UDP port 8676.

Example packet:

{
  "lat": 51.5073,
  "lon": -0.1277,
  "speed": 13.4,
  "sats": 18,
  "hdop": "-"
}


speed is sent in m/s. XRHUD converts it to km/h or mph for display.

Requirements

Android phone with GPS

Android 8.0+ (API 26+)

A network connection between the phone and XRHUD host

XRHUD running on the target machine

The app uses Android's native LocationManager and does not require Google Play Services.

Building

Install the Android SDK with:

Android SDK Platform 35

Android Build Tools

Android command-line tools

Use Java 21 and Gradle 8.9.

Build:

/opt/gradle/gradle-8.9/bin/gradle assembleDebug


The APK will be generated at:

app/build/outputs/apk/debug/app-debug.apk


Install with ADB:

adb install -r app/build/outputs/apk/debug/app-debug.apk

Usage

Connect the phone and XRHUD host to the same network.

Start hud_app.py on the XRHUD host.

Find the host's IP address.

Enter that IP into XRHUDAPP.

Grant precise location permission.

Press START GPS.

The app sends packets to:

<XRHUD_HOST_IP>:8676

Network

The app is not a broadcast service.

It sends UDP packets only to the IP address entered in the app.

This works particularly well when the Android phone is acting as a Wi-Fi hotspot and the XRHUD host is connected to it.

GPS data

The app provides:

Latitude

Longitude

GPS-derived speed

Satellites used in fix

GPS speed is taken directly from Android's Location data rather than calculated by integrating acceleration.

XRHUD integration

The phone handles:

GPS

Speed

The XRHUD host/IMU handles:

Head orientation

Accelerometer/G-force data

HUD rendering

This keeps the IMU-based G-force measurement independent from the GPS speed measurement.
