# Connecting the fingerprint kiosk (ESP32 + R307S + 3.5" TFT)

The firmware (`esp32/fingerprint_attendance.ino`) targets an **ESP32 DevKit programmed from
the Arduino IDE**. A plain Arduino Uno/Nano has no WiFi, so it cannot talk to the backend.

## Parts (per the lab's wiring diagram)
ESP32 DevKit (30/38-pin) · R307S fingerprint sensor · 3.5" TFT LCD shield (480x320, parallel) ·
active buzzer module · 5V 1A SMPS power adapter · LM2596 buck converter module

## Power architecture

```
5V 1A SMPS ──┬─────────────────────────► R307S VCC (5V)
             ├─────────────────────────► Buzzer VCC (5V)
             └──► LM2596 buck converter IN+/IN- ──► OUT+ (trimmed to 3.3V) ──► ESP32 3V3 + TFT shield VCC
```
All GND pins — SMPS, buck converter, R307S, buzzer, ESP32, TFT shield — are tied together.

**Before connecting the buck converter's output to the ESP32**, power it from the SMPS alone
and adjust its trimmer pot until a multimeter across OUT+/OUT- reads **exactly 3.3V**. The
ESP32 and TFT shield are 3.3V logic; feeding them 5V will damage them.

## Wiring

### R307S fingerprint sensor (5V)
| R307S wire | ESP32 pin | Note |
|---|---|---|
| VCC (red) | 5V (from SMPS, not the ESP32's 5V pin) | 3.6–6 V supply |
| GND (black) | GND | common ground |
| TXD (yellow/green) | GPIO16 (RX2) | sensor **TX goes to ESP32 RX** |
| RXD (white/blue) | GPIO17 (TX2) | sensor **RX goes to ESP32 TX** |

### Buzzer (5V)
| Buzzer | ESP32 |
|---|---|
| VCC | 5V (from SMPS) |
| GND | GND |
| SIG | GPIO25 |

### 3.5" TFT LCD shield (3.3V, 8-bit parallel + SPI SD reader)
| LCD pin | ESP32 pin (3.3V) |
|---|---|
| LCD_RST | GPIO4 |
| LCD_CS | GPIO5 |
| LCD_RS (DC) | GPIO2 |
| LCD_WR | GPIO15 |
| LCD_RD | GPIO13 |
| LCD_D0 | GPIO18 |
| LCD_D1 | GPIO19 |
| LCD_D2 | GPIO23 |
| LCD_D3 | GPIO22 |
| LCD_D4 | GPIO21 |
| LCD_D5 | GPIO14 |
| LCD_D6 | GPIO27 |
| LCD_D7 | GPIO26 |
| SD_CS | GPIO33 |
| SD_DI (MOSI) | GPIO23 (shared with LCD_D2 on this shield) |
| SD_DO (MISO) | GPIO19 (shared with LCD_D1) |
| SD_SCK | GPIO18 (shared with LCD_D0) |
| VCC | 3.3V (from the LM2596, not the ESP32's 3V3 pin directly, unless your buck converter can supply both) |
| GND | GND |

Wire colours differ between vendors: trust the labels on your sensor's/shield's datasheet, not
the colours. TX/RX crossed on the R307S is the most common fault ("R307S NOT found" in the
Serial Monitor).

## Arduino IDE setup
1. File → Preferences → add `https://espressif.github.io/arduino-esp32/package_esp32_index.json` to *Additional boards manager URLs*.
2. Boards Manager → install **esp32 by Espressif**. Select board **ESP32 Dev Module**.
3. Library Manager → install **Adafruit Fingerprint Sensor Library**, **ArduinoJson**, and **TFT_eSPI** (by Bodmer).
4. Configure TFT_eSPI for this shield **before** compiling anything — it's set at compile time, not
   in the sketch. Open `esp32/User_Setup_BCI_ILI9488.h`, read its header comment, and either replace
   TFT_eSPI's own `User_Setup.h` with it, or wire it in via `User_Setup_Select.h`. Skipping this
   leaves the screen blank even though the code compiles fine.
5. Edit the four values at the top of the sketch:
   `WIFI_SSID`, `WIFI_PASSWORD`, `BACKEND_BASE_URL` (`http://<PC LAN IP>:8080`, never `localhost`), `DEVICE_API_KEY`.
   Demo mode key: `local-demo-device-key`. Real key: printed in the backend console on first start, or Admin → Devices.
6. Upload, then open Serial Monitor at **115200 baud**, line ending **Newline**.

## Test the hardware together first (recommended)

Before flashing the full kiosk firmware, flash `esp32/hardware-test/hardware_integration_test.ino` -
a standalone sketch with no WiFi/backend that brings up the R307S sensor, the buzzer, and the TFT
together and proves all three work, wired exactly per the diagram above. It shows status for each
piece on screen (sensor OK/FAIL, buzzer test tone, live scan results with a running OK/Failed count)
and prints the same to Serial. See `esp32/hardware-test/README.md` for what to expect and
troubleshooting (blank screen, scrambled colours, sensor not found, no sound). Once that sketch
reports everything working, move on to the main kiosk firmware below - it reuses this exact wiring
and TFT config.

## Connection checklist
1. PC and ESP32 on the **same WiFi**. PC firewall allows inbound TCP **8080**.
2. From another device, open `http://<PC LAN IP>:8080/actuator/health` → must show `{"status":"UP"}`.
3. Serial Monitor shows `WiFi connected` and `R307S found`.
4. Admin → Devices shows the kiosk **ONLINE** within ~30 s (heartbeat).

## Register a person from the browser (recommended)

No Serial Monitor needed for day-to-day use:

1. Admin → **People** → click **Scan finger** (pick the kiosk first if you have more than one).
2. The sketch polls `/api/v1/scan/enrollment-pending` every ~3 seconds; once it sees the
   command it prompts on the TFT/Serial Monitor for the same two-touch scan as manual
   enrollment.
3. On success it reports back automatically and the People page shows a form — fill in the
   name, person type (Student/Lecturer/Rev. Father/Guest) and any other details, then submit.

## Manual enrollment (fallback, via Serial Monitor)
1. Admin → People → "Enter slot manually" shows a free slot, e.g. `3`.
2. Serial Monitor: `enroll 3`, place the same finger twice when prompted.
3. Register the person on the People page with **the same slot number**.

## Test without hardware
```
curl -X POST http://localhost:8080/api/v1/scan -H "X-Device-Key: local-demo-device-key" -H "Content-Type: application/json" -d "{\"fingerprintTemplateId\":1}"
```

## Display
The TFT is driven live via **TFT_eSPI**, configured for this shield's 8-bit parallel ILI9486/ILI9488
panel by `esp32/User_Setup_BCI_ILI9488.h` (see Arduino IDE setup step 4 above). `initDisplay()`,
`drawIdleScreen()` and `drawStatusScreen()` in the sketch call straight into TFT_eSPI - edit those
three functions if you want a different layout, bigger text, or a logo. If colours look wrong or the
image is scrambled, the panel is the other driver than the one selected - swap `ILI9486_DRIVER` /
`ILI9488_DRIVER` in the setup header and re-upload.
