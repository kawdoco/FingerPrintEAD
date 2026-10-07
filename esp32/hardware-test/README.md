# Hardware bring-up test: R307S + buzzer + 3.5" TFT (ILI9486/ILI9488)

Run this **before** flashing the full attendance firmware. It has no WiFi and no
backend calls — it only proves the three pieces of hardware are wired correctly
and work together, wired exactly as in the lab's wiring diagram.

## Files
- `hardware_integration_test.ino` — the test sketch
- `User_Setup_BCI_ILI9488.h` — TFT_eSPI configuration for this shield's pinout

## 1. Install libraries (Arduino IDE -> Library Manager)
- **Adafruit Fingerprint Sensor Library**
- **TFT_eSPI** by Bodmer

## 2. Configure TFT_eSPI (compile-time, not in the .ino)
Open the full header comment at the top of `User_Setup_BCI_ILI9488.h` — it gives two
ways to wire it in. The short version: replace the contents of TFT_eSPI's own
`User_Setup.h` (inside your Arduino libraries folder) with the contents of this file,
then restart the Arduino IDE.

If you skip this step, the sketch still compiles but the screen stays blank or garbled.

## 3. Wire it up
Follow `docs/ESP32_INTEGRATION.md` in the main project (same pin numbers as the
wiring diagram): R307S and buzzer on 5V, ESP32 and TFT shield on 3.3V from the
LM2596 buck converter, all grounds tied together. **Set the LM2596 output to exactly
3.3V with a multimeter before connecting it to the ESP32/TFT** — feeding them 5V
will damage them.

## 4. Upload and watch for, in order
1. TFT lights up with a "BCI Lab - Hardware Test" screen and three status rows.
2. A short beep plays while "Buzzer: TEST" is on screen, then it turns green/"OK" —
   confirms the buzzer and its GPIO25 wiring.
3. "R307S: ..." turns green/"OK" with a two-tone ready chime, or red/"FAIL" with an
   error buzz if the sensor isn't found (see troubleshooting below).
4. Place a finger: screen shows "Scanning...", then either:
   - **green "SCAN OK"** + a rising 3-note beep, or
   - **red "SCAN FAILED"** + two low beeps (try again, clean/dry the finger).
5. A running OK/Failed count stays at the bottom of the screen.

Serial Monitor (115200 baud) mirrors everything, with extra detail (e.g. how many
fingerprints are already stored on the sensor).

## Troubleshooting
| Symptom | Likely cause |
|---|---|
| Screen stays black | TFT_eSPI not configured (step 2), or TFT not getting 3.3V |
| Screen shows garbage/scrambled | Wrong driver — edit `User_Setup_BCI_ILI9488.h`, swap which of `ILI9486_DRIVER` / `ILI9488_DRIVER` is uncommented, re-upload |
| Image upside down or mirrored | Change `tft.setRotation(1)` to `0`, `2`, or `3` in `tftBoot()` |
| "R307S NOT detected" | TXD/RXD swapped, VCC on 3.3V instead of 5V, or a loose GND |
| No sound from buzzer | Check SIG -> GPIO25, VCC -> 5V (not 3.3V), and that it's wired with correct polarity if it's a passive buzzer |
| ESP32 won't flash with the shield plugged in | GPIO2 and GPIO5 are boot-strapping pins the shield uses — unplug the shield while uploading, plug it back in to run |

Once all three show green/OK and a finger scan works reliably, you're ready to flash
`esp32/fingerprint_attendance.ino` from the main project, which uses this exact same
wiring and display config for day-to-day attendance scanning.
