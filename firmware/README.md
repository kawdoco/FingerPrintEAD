# BCI Attendance System — Dinith's Workload
**Role:** Hardware Lead + Firmware + Device Integration
**Stack:** ESP32, R307S Fingerprint Sensor, 3.5" TFT Touchscreen, Buzzer, Arduino C++

This covers **PART A (Hardware)** and **PART B (Firmware)** from the team's workload
plan. Gimsara owns the Spring Boot backend, Sandali owns the Supabase database,
and Thamashi owns the React frontend — this device only needs to know the
backend's URL (see `config.h`).

---

## 1. Components Checklist

- [ ] ESP32 dev board
- [ ] R307S Fingerprint Sensor
- [ ] 3.5" TFT Touchscreen (SPI, e.g. ILI9486/ILI9488 controller)
- [ ] Buzzer (active, 5V or 3.3V)
- [ ] LM2596 Buck Converter
- [ ] 5V power supply
- [ ] Jumper wires / breadboard or perfboard

---

## 2. Wiring / Pin Map

All pin numbers below are also defined in `BCI_Attendance/config.h` — change
them together if you rewire anything.

### 2.1 Power System
```
5V Power Supply → LM2596 (buck converter) → 5V rail (TFT, buzzer)
                                           → 3.3V rail (ESP32, R307S logic)
```
Before connecting any module, verify with a multimeter:
- Output voltage of the LM2596 is correct (5V and/or 3.3V as needed)
- All grounds (ESP32, R307S, TFT, buzzer, power supply) share a **common GND**
- No loose or shorted connections

### 2.2 ESP32
- Power via USB (for programming/testing) or via the regulated 5V rail (VIN) for standalone operation.

### 2.3 R307S Fingerprint Sensor (UART, HardwareSerial2)
| R307S Pin | ESP32 Pin | Notes |
|---|---|---|
| VCC | 3.3V or 5V (check your module's spec) | |
| GND | GND | common ground |
| TX  | GPIO 16 (RX2) | |
| RX  | GPIO 17 (TX2) | |

### 2.4 3.5" TFT Display + Touch (SPI)
| TFT Pin | ESP32 Pin |
|---|---|
| MOSI (SDI) | GPIO 23 |
| MISO (SDO) | GPIO 19 |
| SCLK | GPIO 18 |
| CS   | GPIO 5 |
| DC / RS | GPIO 2 |
| RST  | GPIO 4 |
| BL (Backlight) | GPIO 32 |
| Touch CS | GPIO 15 |
| VCC | 3.3V |
| GND | GND |

> These pins must also be set in the **TFT_eSPI** library's `User_Setup.h`
> (see Section 4 below) — the library reads pins at compile time, not from
> `config.h`.

### 2.5 Buzzer
| Buzzer Pin | ESP32 Pin |
|---|---|
| Signal (+) | GPIO 25 |
| GND (−) | GND |

---

## 3. Hardware Assembly Steps (Part A)

1. **Prepare all components** — gather everything in the checklist above.
2. **Connect the power system** — 5V supply → LM2596 → regulated rails. Check
   voltage, common ground, and connections before powering anything else.
3. **Connect ESP32** — power it, test the USB connection, upload a basic
   blink/test sketch to confirm the board is alive.
4. **Connect R307S** — wire VCC/GND/TX/RX, then test fingerprint communication
   (see Section 6, `fingerprintInit()` should print sensor capacity to Serial).
5. **Connect TFT display** — wire SPI pins, configure `TFT_eSPI`, test screen output.
6. **Connect touchscreen** — configure touch pins, test raw touch coordinates,
   run `touchCalibrate()` once and copy the printed values into `calData[]`
   in `display_functions.cpp`.
7. **Connect buzzer** — wire the signal pin, test both the short success beep
   and the long error beep.
8. **Complete hardware testing** — go through the checklist in Section 7 below
   before moving on to full firmware testing.

---

## 4. Firmware Setup

### 4.1 Arduino IDE / Board Setup
1. Install the **ESP32 board package** (Boards Manager → search "esp32").
2. Select your specific ESP32 board under Tools → Board.
3. Open `BCI_Attendance/BCI_Attendance.ino`.

### 4.2 Required Libraries
Install via Arduino IDE → Tools → Manage Libraries:
- **Adafruit Fingerprint Sensor Library**
- **TFT_eSPI** (by Bodmer)
- WiFi, HTTPClient, time.h — bundled with the ESP32 board package, no install needed.

### 4.3 TFT_eSPI Configuration (required!)
`TFT_eSPI` needs pin definitions set **inside the library itself**:
1. Locate the library folder (usually `Documents/Arduino/libraries/TFT_eSPI`).
2. Open `User_Setup.h` (or create a custom setup and select it in
   `User_Setup_Select.h`).
3. Set the driver (e.g. `#define ILI9486_DRIVER`, match your actual panel) and
   the pin definitions to match Section 2.4 above:
   ```cpp
   #define TFT_MOSI 23
   #define TFT_MISO 19
   #define TFT_SCLK 18
   #define TFT_CS    5
   #define TFT_DC    2
   #define TFT_RST   4
   #define TOUCH_CS 15
   ```
4. Save and re-verify/compile.

### 4.4 Project Config
Open `BCI_Attendance/config.h` and fill in:
- `WIFI_SSID` / `WIFI_PASSWORD`
- `SERVER_BASE_URL` — Gimsara's Spring Boot server address (local IP:port or deployed URL)
- `DEVICE_ID` — must match the device row Gimsara/Sandali register in the database

### 4.5 File Layout
```
firmware/
└── BCI_Attendance/
    ├── BCI_Attendance.ino        Main state machine (setup/loop)
    ├── config.h                  Wi-Fi, server, pins, device settings
    ├── fingerprint_functions.h/.cpp   Init, scan, verify, enroll, delete
    ├── display_functions.h/.cpp       TFT screens + touch
    ├── buzzer_functions.h/.cpp        Success/error feedback
    └── network_functions.h/.cpp       Wi-Fi, NTP, HTTP POST to backend
```

---

## 5. How the Firmware Works

State machine in `BCI_Attendance.ino`:

```
BOOTING → WELCOME → SCANNING → SENDING → SUCCESS → (back to) WELCOME
                        │                     
                        └──(no match/error)──→ ERROR_STATE → WELCOME
Touch top-right corner while idle → ENROLLING → WELCOME
```

Data flow on a successful scan:
```
Finger placed on R307S
   ↓
ESP32 verifies against stored templates
   ↓
TFT shows "Welcome, Fingerprint ID X"
   ↓
JSON POST over Wi-Fi to Spring Boot: /api/attendance
   { "deviceId": "...", "fingerprintId": 12, "timestamp": "..." }
```

A background heartbeat (`sendHeartbeat()`, every 60s) POSTs to
`/api/devices/heartbeat` so Thamashi's Device Monitoring page can show
Online/Offline + Last Seen — confirm this exact endpoint with Gimsara,
or point it at whatever heartbeat route the backend actually exposes.

---

## 6. Testing While Developing

- **Serial Monitor at 115200 baud** shows Wi-Fi status, NTP sync, fingerprint
  sensor detection, HTTP response codes, and enrollment progress — check here
  first if something isn't working.
- Test fingerprint enrollment/scan by touching the top-right ~60x40px corner
  of the screen while on the Welcome screen to trigger enrollment mode.
- If `fingerprintInit()` fails, double-check TX/RX aren't swapped (a very
  common R307S wiring mistake) and that baud rate matches (`57600` default).

---

## 7. Hardware Testing Checklist (Part A, Step 8)

- [ ] ESP32 boots and runs a test program
- [ ] Fingerprint sensor responds (`verifyPassword()` succeeds, capacity printed)
- [ ] TFT display shows output correctly (colors, orientation)
- [ ] Touchscreen reports accurate coordinates after calibration
- [ ] Buzzer plays both success (short) and error (long) tones
- [ ] Power system delivers correct, stable voltage under load

## 8. Firmware Testing Checklist (Part B, Step 8)

- [ ] Fingerprint → ESP32 → TFT message pipeline works end-to-end
- [ ] Wi-Fi connects on boot and auto-reconnects if dropped
- [ ] NTP time sync succeeds (timestamps are not `1970-01-01`)
- [ ] Successful scan sends a correctly-formatted POST to `/api/attendance`
      and Gimsara confirms it's received on the backend
- [ ] Unrecognized fingerprint shows an error screen + long beep, no POST sent
- [ ] Enrollment flow stores a new template and it can be matched afterward
- [ ] Heartbeat POST reaches the backend and Thamashi's dashboard reflects it

---

## 9. Notes for Team Sync

- Confirm with **Gimsara** the exact request/response shape for
  `/api/attendance` and whether a heartbeat/device-status endpoint exists —
  `network_functions.cpp` currently assumes `POST /api/devices/heartbeat`.
- Confirm with **Sandali** that `fingerprintId` sent by the device matches the
  `fingerprint_slot` value stored in the Fingerprints table, so a scan maps
  back to the correct user.
- `DEVICE_ID` in `config.h` must exactly match the `device_code`/`id` Sandali
  seeds in the Devices table.
