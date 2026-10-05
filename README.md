# XRHUDAPP – Android Companion for XRHUD  

A tiny, AOSP‑compatible Android app that reads the phone’s GPS location and speed, then streams the data over UDP to the XRHUD host (`hud_app.py`) running on a Steam Deck.

---  

## How It Works  

1. **Phone side** – XRHUDAPP fetches latitude, longitude, speed, satellite count and HDOP from Android’s native `LocationManager`.  
2. **Transmission** – The data is packed into a JSON message and sent via UDP to the XRHUD host on **port 8676**.  
3. **Deck side** – `hud_app.py` receives the packets, converts the speed to the user‑selected unit (km/h or mph) and overlays the information on the Xreal Air HUD.  

```
Phone (XRHUDAPP) ──► UDP JSON (port 8676) ──► Steam Deck (hud_app.py)
```

---  

## Packet Format  

```json
{
  "lat": 51.5073,
  "lon": -0.1277,
  "speed": 13.4,
  "sats": 18,
  "hdop": "-"
}
```

| Field | Meaning |
|-------|---------|
| **lat**   | Latitude in decimal degrees |
| **lon**   | Longitude in decimal degrees |
| **speed** | GPS speed in metres per second (converted by XRHUD) |
| **sats**  | Number of satellites used for the fix |
| **hdop**  | Horizontal Dilution of Precision (may be “‑”) |

---  

## Requirements  

| Requirement | Minimum |
|-------------|---------|
| Android device with GPS | Android 8.0 (API 26) or newer |
| Network | Phone and Steam Deck must share the same LAN (a Wi‑Fi hotspot works) |
| XRHUD host | `hud_app.py` running and listening on UDP **8676** |
| Google Play Services | **Not required** – the app uses the built‑in `LocationManager` |

---  

## Building the APK  

### Prerequisites  

* Android SDK Platform 35  
* Android Build Tools (latest)  
* Command‑line tools (`sdkmanager`, `avdmanager`, …)  
* **Java 21**  
* **Gradle 8.9**  

### Build steps  

```bash
# From the repository root
/opt/gradle/gradle-8.9/bin/gradle assembleDebug
```

The debug APK will be generated at  

```
app/build/outputs/apk/debug/app-debug.apk
```

---  

## Installing with ADB  

```bash
# 1. Enable “USB debugging” on the phone.
# 2. Connect the phone via USB.
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app is now installed and ready to launch.

---  

## Running the App  

1. **Network** – Put the phone and Steam Deck on the same Wi‑Fi network (e.g., enable a hotspot on the phone).  
2. **Start XRHUD** – Run `hud_app.py` on the Deck and note its IP address.  
3. **Configure XRHUDAPP** – Open the app, enter the Deck’s IP, and grant **Precise location** permission.  
4. **Activate GPS** – Tap **START GPS**.  
5. The app will begin sending UDP packets to `<XRHUD_HOST_IP>:8676`.  

### Verifying the connection (on the Deck)

```bash
# Listen for incoming packets
sudo tcpdump -ni any udp port 8676

# Or check the listening socket
ss -lunp | grep 8676
```

You should see a steady stream of JSON packets whenever the phone has a GPS fix.

---  

## Network Behaviour  

* **Unicast only** – XRHUDAPP sends packets solely to the IP you entered; no broadcast traffic.  
* **Port** – Fixed at **8676**; the host must listen on this port.  

---  

## GPS Data Details  

* **Latitude / Longitude** – Decimal degrees from the GPS fix.  
* **Speed** – Directly taken from the GPS sensor (m / s); not derived from accelerometer integration, so no drift.  
* **Satellites / HDOP** – Optional fields that give fix quality information.  

---  

## Integration Overview  

```
Android Phone (XRHUDAPP) ──► UDP 8676 ──► Steam Deck (hud_app.py)
      │                     │
      ├─► Latitude          ├─► HUD overlay
      ├─► Longitude         ├─► Unit conversion (km/h ↔ mph)
      ├─► Speed (m/s)       └─► Combined with IMU data
      └─► Sat / HDOP
```

*The XRHUD host handles head orientation, G‑force calculation, HUD rendering, and speed‑unit conversion. The phone supplies only positional and speed data, keeping the two sensor streams independent.*

---  

## Project Structure  

```
XRHUDAPP/
├─ app/
│  ├─ build.gradle
│  └─ src/
│     └─ main/
│        ├─ AndroidManifest.xml
│        └─ java/
│           └─ com/
│              └─ xrhud/
│                 └─ phone/
│                    ├─ GpsService.java
│                    └─ MainActivity.java
├─ build.gradle
└─ settings.gradle
```

---  

## Related Project  

* **XRHUD (Steam Deck HUD)** – https://github.com/3x65Fork1/XRHUD  

---  

## License  

MIT License – you may use, copy, modify, merge, publish, distribute, sublicense, and sell this software, provided the original copyright notice and license are retained.
