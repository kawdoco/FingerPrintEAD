/*
 * BCI Research Lab - Hardware integration test
 * R307S fingerprint sensor + buzzer + 3.5" TFT (ILI9486/ILI9488, 480x320 parallel)
 *
 * Purpose: confirm all THREE pieces of hardware work together, wired exactly as in
 * "FINGERPRINT ATTENDANCE SYSTEM - WIRING DIAGRAM (5V & 3.3V POWER)", before plugging
 * this sketch's logic into the full WiFi/backend firmware. No WiFi, no backend calls here.
 *
 * Libraries needed (Arduino IDE -> Library Manager):
 *   "Adafruit Fingerprint Sensor Library"
 *   "TFT_eSPI" by Bodmer
 *
 * IMPORTANT - TFT_eSPI is configured at COMPILE TIME, not in this .ino file.
 * Before uploading, set up User_Setup_BCI_ILI9488.h as described in that file's
 * header comment (replace TFT_eSPI's User_Setup.h, or wire it in via
 * User_Setup_Select.h). If you skip this the screen will stay blank/garbled
 * even though the code compiles.
 *
 * What you should see:
 *   1. TFT lights up and shows a "Hardware Test" boot screen with three status lines.
 *   2. Serial Monitor (115200 baud) prints the same status, plus a startup beep.
 *   3. Sensor line goes green/"OK" if the R307S is found; otherwise red/"FAIL" and
 *      the sketch stops there (fix wiring before continuing - see troubleshooting below).
 *   4. Placing a finger -> short beep + "Scanning..." on screen -> either
 *      "SCAN OK" (ascending 3-note beep) or "SCAN FAILED" (two low beeps).
 *   5. A running count of successful/failed scans stays on screen.
 */

#include <SPI.h>
#include <TFT_eSPI.h>
#include <Adafruit_Fingerprint.h>

// ---------------------------------------------------------------------------
HardwareSerial fingerSerial(2);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&fingerSerial);
TFT_eSPI tft = TFT_eSPI();

#define BUZZER_PIN 25

// R307S  (per wiring diagram: TXD -> ESP32 GPIO16 (RX2), RXD -> ESP32 GPIO17 (TX2))
#define FINGER_RX_PIN 16
#define FINGER_TX_PIN 17

int okCount = 0;
int failCount = 0;

// ------------------------------- buzzer -------------------------------------
void beep(int frequency, int duration) {
  tone(BUZZER_PIN, frequency);
  delay(duration);
  noTone(BUZZER_PIN);
}

void readySound() {          // sensor found, system ready
  beep(1800, 150);
  delay(80);
  beep(2500, 250);
}

void scanningSound() {       // finger detected, image being captured
  beep(3000, 100);
}

void successSound() {        // image captured cleanly
  beep(2500, 120);
  delay(80);
  beep(3200, 120);
  delay(80);
  beep(4000, 250);
}

void errorSound() {          // sensor missing, or a scan failed
  beep(800, 300);
  delay(100);
  beep(800, 300);
}

// -------------------------------- TFT ----------------------------------------
#define BG      TFT_BLACK
#define FG      TFT_WHITE
#define DIM     TFT_DARKGREY
#define GREEN   TFT_GREEN
#define RED     TFT_RED
#define AMBER   TFT_ORANGE
#define CYAN    TFT_CYAN

void tftBoot() {
  tft.init();
  tft.setRotation(1);           // landscape; use 3 to flip 180 degrees if upside down
  tft.fillScreen(BG);
  tft.setTextDatum(TL_DATUM);

  tft.setTextColor(FG, BG);
  tft.setTextSize(2);
  tft.drawString("BCI Lab - Hardware Test", 10, 10);
  tft.drawFastHLine(10, 34, tft.width() - 20, DIM);

  tft.setTextSize(2);
  tft.drawString("TFT", 10, 50);
  drawStatus(120, 50, "OK", GREEN);     // if you can read this, the TFT already works
}

// Clears a fixed-width area and writes a status word in a colour - used for
// the sensor/buzzer/TFT status lines and for the scan counters.
void drawStatus(int x, int y, const char* text, uint16_t color) {
  tft.fillRect(x, y, 140, 20, BG);
  tft.setTextColor(color, BG);
  tft.setTextSize(2);
  tft.drawString(text, x, y);
}

void drawLabel(int x, int y, const char* text) {
  tft.setTextColor(FG, BG);
  tft.setTextSize(2);
  tft.drawString(text, x, y);
}

void drawBigMessage(const char* line1, const char* line2, uint16_t color) {
  tft.fillRect(0, 130, tft.width(), 90, BG);
  tft.setTextColor(color, BG);
  tft.setTextSize(3);
  tft.setTextDatum(MC_DATUM);
  tft.drawString(line1, tft.width() / 2, 160);
  tft.setTextSize(2);
  tft.drawString(line2, tft.width() / 2, 195);
  tft.setTextDatum(TL_DATUM);
}

void drawCounts() {
  tft.fillRect(10, 240, tft.width() - 20, 24, BG);
  tft.setTextColor(GREEN, BG);
  tft.setTextSize(2);
  char buf[32];
  snprintf(buf, sizeof(buf), "OK: %d", okCount);
  tft.drawString(buf, 10, 240);
  tft.setTextColor(RED, BG);
  snprintf(buf, sizeof(buf), "Failed: %d", failCount);
  tft.drawString(buf, 150, 240);
}

// --------------------------------- setup --------------------------------------
void setup() {
  Serial.begin(115200);
  delay(200);
  pinMode(BUZZER_PIN, OUTPUT);

  Serial.println();
  Serial.println("==============================================");
  Serial.println(" BCI LAB - FINGERPRINT + BUZZER + TFT HW TEST");
  Serial.println("==============================================");

  tftBoot();
  drawLabel(10, 75, "Buzzer");
  drawStatus(120, 75, "TEST", AMBER);
  beep(1200, 120);                       // if you hear this, the buzzer wiring is correct
  drawStatus(120, 75, "OK", GREEN);

  drawLabel(10, 100, "R307S");
  drawStatus(120, 100, "...", AMBER);

  fingerSerial.begin(57600, SERIAL_8N1, FINGER_RX_PIN, FINGER_TX_PIN);
  finger.begin(57600);

  Serial.println("Checking fingerprint sensor...");
  if (finger.verifyPassword()) {
    finger.getTemplateCount();
    Serial.println("SUCCESS: R307S detected!");
    Serial.printf("Fingerprints already stored on sensor: %d\n", finger.templateCount);

    drawStatus(120, 100, "OK", GREEN);
    readySound();

    drawBigMessage("Place your finger", "Waiting for a scan...", CYAN);
    drawCounts();
  } else {
    Serial.println("ERROR: R307S NOT detected!");
    Serial.println("Check: TXD/RXD crossed? 5V on VCC (not 3.3V)? Common GND with ESP32?");

    drawStatus(120, 100, "FAIL", RED);
    drawBigMessage("Sensor not found", "Check wiring, see Serial", RED);
    errorSound();

    while (1) { delay(1000); }           // stop here - fix the sensor wiring first
  }
}

// ---------------------------------- loop ---------------------------------------
void loop() {
  uint8_t result = finger.getImage();

  if (result == FINGERPRINT_OK) {
    Serial.println();
    Serial.println("FINGER DETECTED - scanning...");
    drawBigMessage("Scanning...", "Hold still", AMBER);
    scanningSound();

    result = finger.image2Tz();

    if (result == FINGERPRINT_OK) {
      Serial.println("SCAN OK - image captured cleanly.");
      okCount++;
      drawBigMessage("SCAN OK", "Image captured", GREEN);
      successSound();
    } else {
      Serial.println("SCAN FAILED - image unclear, try again.");
      failCount++;
      drawBigMessage("SCAN FAILED", "Try again - clean finger?", RED);
      errorSound();
    }
    drawCounts();

    Serial.println("Remove finger...");
    while (finger.getImage() != FINGERPRINT_NOFINGER) {
      delay(50);
    }

    Serial.println("Ready for next scan.");
    drawBigMessage("Place your finger", "Waiting for a scan...", CYAN);
    delay(300);
  }

  delay(100);
}
