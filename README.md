XRHUDAPP

A small AOSP-compatible Android companion app for XRHUD.

XRHUDAPP reads the phone's GPS location and GPS-derived speed, then sends the data over UDP to the XRHUD host running hud_app.py.

How It Works
┌────────────────────┐
│    Android Phone   │
│                    │
│   GPS + Speed      │
│                    │
│     XRHUDAPP       │
└─────────┬──────────┘
          │
          │ UDP / JSON
          │ Port 8676
          ▼
┌────────────────────┐
│    XRHUD Host      │
│    Steam Deck      │
│                    │
│     hud_app.py     │
└─────────┬──────────┘
          │
          ├── GPS / Location
          │
          └── Speed


The phone sends GPS data directly to the configured target IP on UDP port 8676.

Packet Format

XRHUDAPP sends JSON packets similar to:

{
  "lat": 51.5073,
  "lon": -0.1277,
  "speed": 13.4,
  "sats": 18,
  "hdop": "-"
}


Where:

Field	Description
lat	Latitude in decimal degrees
lon	Longitude in decimal degrees
speed	GPS speed in metres per second
sats	Satellites currently used in the fix
hdop	HDOP value, where available

The speed value is supplied in m/s. XRHUD converts it to km/h or mph for display.

Requirements

Android phone with GPS

Android 8.0+ / API 26+

Network connection between the phone and XRHUD host

XRHUD running on the target machine

The app uses Android's native LocationManager and does not require Google Play Services.

Building
Android SDK

Install:

Android SDK Platform 35

Android Build Tools

Android command-line tools

Java and Gradle

The project is built using:

Java 21

Gradle 8.9

Build the debug APK with:

/opt/gradle/gradle-8.9/bin/gradle assembleDebug


The APK will be generated at:

app/build/outputs/apk/debug/app-debug.apk

Install with ADB

Connect the Android phone with USB debugging enabled:

adb install -r app/build/outputs/apk/debug/app-debug.apk

Usage

Connect the phone and XRHUD host to the same network.

Start hud_app.py on the XRHUD host.

Find the XRHUD host's IP address.

Enter the host IP into XRHUDAPP.

Grant precise location permission.

Press START GPS.

The app sends packets to:

<XRHUD_HOST_IP>:8676

Example Network

A phone hotspot can be used as the network:

┌────────────────────┐
│    Android Phone   │
│                    │
│   Wi-Fi Hotspot    │
│   GPS + XRHUDAPP   │
└─────────┬──────────┘
          │
          │ Wi-Fi
          ▼
┌────────────────────┐
│    Steam Deck      │
│                    │
│    hud_app.py      │
│    UDP :8676       │
└────────────────────┘


The XRHUD host's IP address is entered into the Android app.

Network Behavior

XRHUDAPP is not a broadcast service.

It sends UDP packets only to the IP address entered by the user:

<XRHUD_HOST_IP>:8676


It does not send GPS data to every device on the network.

Verifying the Connection

On the XRHUD host, listen for UDP packets:

sudo tcpdump -ni any udp port 8676


When XRHUDAPP is running and has a GPS fix, packets should appear.

You can also check whether XRHUD is listening on the expected port:

ss -lunp | grep 8676

GPS Data

XRHUDAPP provides:

Latitude

Longitude

GPS-derived speed

Satellites used in the GPS fix

GPS speed is taken directly from Android's Location data.

It is not calculated by integrating accelerometer data.

This avoids the accumulated drift that occurs when trying to derive vehicle speed from an accelerometer alone.

XRHUD Integration

The system separates the phone's GPS system from the Xreal Air IMU.

             XRHUD SYSTEM
                  │
       ┌──────────┴──────────┐
       │                     │
       ▼                     ▼
 Android GPS             Xreal Air IMU
       │                     │
       │                     ├── Orientation
       │                     ├── Pitch / Bank
       │                     └── G-force
       │
       ├── Location
       └── Speed
       │
       └──────────┐
                  ▼
              hud_app.py
                  │
                  ▼
             Xreal Air HUD

Android Phone

The phone handles:

GPS location

GPS-derived speed

Satellite information

UDP transmission

XRHUD Host / IMU

The XRHUD host handles:

Head orientation

Accelerometer/G-force data

HUD rendering

Speed unit conversion

GPS display

This keeps the IMU-based G-force measurement independent from GPS speed.

Project Structure
XRHUDAPP/
├── app/
│   ├── build.gradle
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/
│               └── com/
│                   └── xrhud/
│                       └── phone/
│                           ├── GpsService.java
│                           └── MainActivity.java
├── build.gradle
└── settings.gradle

Related Project

XRHUDAPP is designed to work with the main XRHUD project:

XRHUD: https://github.com/3x65Fork1/XRHUD

License

This project is licensed under the MIT License.

You are free to use, copy, modify, merge, publish, distribute, sublicense, and sell this software, provided that the original copyright notice and license are retained.
