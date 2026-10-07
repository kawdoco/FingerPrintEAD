/*
 * BCI Research Lab - Fingerprint Attendance Kiosk
 * Board : ESP32 DevKit                Sensor : R307S on UART2 (GPIO16 RX2 / GPIO17 TX2), 5V
 * Buzzer: GPIO25, 5V                  Display: 3.5" TFT parallel shield (480x320), 3.3V - see PIN MAP below
 *
 * Power (per "FINGERPRINT ATTENDANCE SYSTEM - WIRING DIAGRAM (5V & 3.3V POWER)"):
 *   5V 1A SMPS -> R307S (VCC) and buzzer (VCC) directly
 *              -> LM2596 buck converter IN+/IN- -> trimmed to OUT+ 3.3V -> ESP32 3V3 pin + TFT shield VCC
 *   All GND pins (SMPS, buck converter, R307S, buzzer, ESP32, TFT) are common.
 *   Set the LM2596 output to exactly 3.3V with a meter BEFORE connecting it to the ESP32.
 *
 * Normal operation
 *   finger placed -> sensor matches -> slot number -> POST /api/v1/scan -> name + check-in/out on screen, buzzer beep
 *
 * Enrollment - two ways, both store into the same sensor:
 *   1. From the admin dashboard's "People" page, click "Scan finger". This sketch polls for that
 *      command, prompts on the TFT, captures the two-touch scan, and reports the result back -
 *      no typing required. This is the flow BCI Lab uses day to day.
 *   2. Manual fallback over the Serial Monitor (115200 baud, "Newline" line ending):
 *        enroll <slot>   store a new fingerprint in slot 1..127
 *        delete <slot>   remove one fingerprint
 *        empty           remove ALL fingerprints from the sensor
 *        count           how many fingerprints are stored
 *
 * Libraries (Library Manager): "Adafruit Fingerprint Sensor Library", "ArduinoJson" (v6 or v7), "TFT_eSPI"
 * TFT: driven live via TFT_eSPI. Configure it FIRST using esp32/User_Setup_BCI_ILI9488.h (see that
 * file's header comment) or the screen will stay blank even though this sketch compiles fine.
 * New to this hardware combo? Flash esp32/hardware-test/hardware_integration_test.ino first - it
 * brings up the sensor + buzzer + screen together with no WiFi/backend involved, to confirm wiring.
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <HardwareSerial.h>
#include <Adafruit_Fingerprint.h>
#include <SPI.h>
#include <TFT_eSPI.h>         // configure via esp32/User_Setup_BCI_ILI9488.h - see that file's header comment

// ---------------------------------------------------------------------------
// FILL THESE IN BEFORE FLASHING
// ---------------------------------------------------------------------------
const char* WIFI_SSID        = "YOUR_WIFI_NAME";
const char* WIFI_PASSWORD    = "YOUR_WIFI_PASSWORD";

// IP address of the PC/server running the Spring Boot backend (NOT "localhost").
// Windows: run `ipconfig` and use the IPv4 address, e.g. http://192.168.1.50:8080
const char* BACKEND_BASE_URL = "http://192.168.1.50:8080";

// Key printed in the backend console on first start, or shown once on Admin -> Devices.
// (Local demo profile uses the key: local-demo-device-key)
const char* DEVICE_API_KEY   = "PASTE_DEVICE_API_KEY_HERE";
// ---------------------------------------------------------------------------

// ------------------------------- PIN MAP ------------------------------------
// R307S fingerprint sensor (5V)
#define FINGERPRINT_RX_PIN 16   // ESP32 RX2  <- R307S TXD
#define FINGERPRINT_TX_PIN 17   // ESP32 TX2  -> R307S RXD

// Buzzer module (5V)
#define BUZZER_PIN         25

// 3.5" TFT LCD shield, SPI-style control lines over an 8-bit parallel bus (3.3V).
// Reference only until a TFT library is wired in (see initDisplay()/drawIdleScreen()/drawStatusScreen()).
#define TFT_RST   4
#define TFT_CS    5
#define TFT_RS    2   // a.k.a. DC
#define TFT_WR    15
#define TFT_RD    13
#define TFT_D0    18
#define TFT_D1    19
#define TFT_D2    23
#define TFT_D3    22
#define TFT_D4    21
#define TFT_D5    14
#define TFT_D6    27
#define TFT_D7    26
// On-shield SD card reader (not used by this sketch, listed for completeness)
#define SD_CS_PIN   33
#define SD_MOSI_PIN 23   // shared with TFT_D2 on this shield's routing
#define SD_MISO_PIN 19   // shared with TFT_D1
#define SD_SCK_PIN  18   // shared with TFT_D0
// -----------------------------------------------------------------------------

HardwareSerial fingerSerial(2);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&fingerSerial);
TFT_eSPI tft = TFT_eSPI();

unsigned long lastHeartbeat = 0;
const unsigned long HEARTBEAT_MS = 30000;

unsigned long lastEnrollmentPoll = 0;
const unsigned long ENROLLMENT_POLL_MS = 3000;

// ----------------------------- setup / loop --------------------------------
void setup() {
  Serial.begin(115200);
  pinMode(BUZZER_PIN, OUTPUT);

  fingerSerial.begin(57600, SERIAL_8N1, FINGERPRINT_RX_PIN, FINGERPRINT_TX_PIN);
  finger.begin(57600);
  if (finger.verifyPassword()) {
    finger.getTemplateCount();
    Serial.printf("R307S found, %d fingerprint(s) stored.\n", finger.templateCount);
    beepReady();
  } else {
    Serial.println("R307S NOT found - check wiring (TX/RX crossed? 5V?).");
    beepError();
  }

  initDisplay();
  drawIdleScreen("Connecting WiFi...");
  connectWiFi();
  drawIdleScreen("Scan your finger");
  Serial.println("Commands: enroll <slot> | delete <slot> | empty | count");
}

void loop() {
  handleSerialCommands();
  ensureWiFi();

  if (millis() - lastHeartbeat > HEARTBEAT_MS) {
    sendHeartbeat();
    lastHeartbeat = millis();
  }

  if (millis() - lastEnrollmentPoll > ENROLLMENT_POLL_MS) {
    lastEnrollmentPoll = millis();
    checkForWebEnrollmentCommand();
  }

  int slot = pollForFingerMatch();
  if (slot > 0) {
    Serial.printf("Matched slot %d\n", slot);
    postScanEvent(slot);
    delay(2500);                       // let the person read the screen
    drawIdleScreen("Scan your finger");
  }
  delay(100);
}

// ------------------------------- WiFi --------------------------------------
void connectWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 20000) {
    delay(400);
    Serial.print(".");
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWiFi connected, IP " + WiFi.localIP().toString());
  } else {
    Serial.println("\nWiFi failed - will keep retrying in the background.");
  }
}

void ensureWiFi() {
  static unsigned long lastTry = 0;
  if (WiFi.status() != WL_CONNECTED && millis() - lastTry > 10000) {
    lastTry = millis();
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  }
}

// --------------------------- fingerprint matching ---------------------------
// Returns matched slot (1..127) or -1 when no finger / no match.
int pollForFingerMatch() {
  uint8_t p = finger.getImage();
  if (p != FINGERPRINT_OK) return -1;          // no finger on the glass

  beepScanning();
  drawStatusScreen("Scanning...", "Hold still", true);

  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) return -1;          // poor image, try again

  p = finger.fingerSearch();
  if (p == FINGERPRINT_OK) return finger.fingerID;

  if (p == FINGERPRINT_NOTFOUND) {
    beepNotRecognized();
    drawStatusScreen("Not recognized", "Try again or ask the coordinator", false);
    delay(1800);
    drawIdleScreen("Scan your finger");
  }
  return -1;
}

// ------------------------------ backend calls -------------------------------
void postScanEvent(int slot) {
  if (WiFi.status() != WL_CONNECTED) {
    drawStatusScreen("No WiFi", "Scan not recorded", false);
    beepError();
    return;
  }

  HTTPClient http;
  http.setTimeout(8000);
  http.begin(String(BACKEND_BASE_URL) + "/api/v1/scan");
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Key", DEVICE_API_KEY);

  JsonDocument body;                            // ArduinoJson v7 (v6: use StaticJsonDocument<128>)
  body["fingerprintTemplateId"] = slot;
  body["checkType"] = "AUTO";                   // first scan of the day = check in, next = check out
  String payload;
  serializeJson(body, payload);

  int code = http.POST(payload);
  String response = http.getString();
  http.end();

  if (code == 200) {
    JsonDocument doc;
    deserializeJson(doc, response);
    const char* name      = doc["data"]["personName"] | "Person";
    const char* checkType = doc["data"]["checkType"]  | "CHECK_IN";
    bool duplicate        = doc["data"]["duplicate"]  | false;

    if (duplicate) {
      drawStatusScreen(name, "Already recorded", true);
      beepDuplicate();
    } else if (strcmp(checkType, "CHECK_OUT") == 0) {
      drawStatusScreen(name, "Checked OUT - goodbye", true);
      beepOk();
    } else {
      drawStatusScreen(name, "Checked IN - welcome", true);
      beepOk();
    }
  } else if (code == 404) {
    drawStatusScreen("Not enrolled", "Finger not linked to anyone", false);
    beepError();
  } else if (code == 401) {
    drawStatusScreen("Device key rejected", "Check DEVICE_API_KEY", false);
    beepError();
  } else if (code == 422) {
    drawStatusScreen("Not allowed", "Record is inactive", false);
    beepError();
  } else {
    Serial.printf("Scan POST failed, HTTP %d: %s\n", code, response.c_str());
    drawStatusScreen("Server error", "Could not record scan", false);
    beepError();
  }
}

void sendHeartbeat() {
  if (WiFi.status() != WL_CONNECTED) return;
  HTTPClient http;
  http.setTimeout(5000);
  http.begin(String(BACKEND_BASE_URL) + "/api/v1/scan/heartbeat");
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Key", DEVICE_API_KEY);
  int code = http.POST("{}");
  if (code != 200) Serial.printf("Heartbeat HTTP %d\n", code);
  http.end();
}

// ------------------- web-triggered enrollment ("Scan finger" button) --------
// The admin dashboard's People page POSTs /api/v1/enrollment/start, which creates
// a session for this device. We poll for it here; when one shows up we run the
// same two-touch capture as the manual "enroll" command, then report back.
void checkForWebEnrollmentCommand() {
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  http.setTimeout(6000);
  http.begin(String(BACKEND_BASE_URL) + "/api/v1/scan/enrollment-pending");
  http.addHeader("X-Device-Key", DEVICE_API_KEY);
  int code = http.GET();
  String response = http.getString();
  http.end();

  if (code != 200) {
    if (code > 0) Serial.printf("Enrollment poll HTTP %d\n", code);
    return;
  }

  JsonDocument doc;
  if (deserializeJson(doc, response) != DeserializationError::Ok) return;
  if (doc["data"].isNull()) return;              // nothing pending right now

  long sessionId  = doc["data"]["id"];
  int  templateId = doc["data"]["templateId"];

  Serial.printf("Web enrollment requested: session %ld, slot %d\n", sessionId, templateId);
  performWebEnrollment(sessionId, (uint8_t)templateId);
}

bool performWebEnrollment(long sessionId, uint8_t slot) {
  int p = -1;
  drawIdleScreen("Register: place finger");
  Serial.printf("Web enrollment - place finger for slot %d...\n", slot);

  unsigned long start = millis();
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (millis() - start > 20000) {
      reportEnrollmentResult(sessionId, false, "Timed out waiting for a finger");
      drawIdleScreen("Scan your finger");
      return false;
    }
    delay(50);
  }
  if (finger.image2Tz(1) != FINGERPRINT_OK) {
    reportEnrollmentResult(sessionId, false, "First scan was unclear - try again");
    drawIdleScreen("Scan your finger");
    return false;
  }

  drawIdleScreen("Remove finger");
  delay(1500);
  while (finger.getImage() != FINGERPRINT_NOFINGER) delay(50);

  drawIdleScreen("Place same finger again");
  p = -1;
  start = millis();
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (millis() - start > 20000) {
      reportEnrollmentResult(sessionId, false, "Timed out on the second scan");
      drawIdleScreen("Scan your finger");
      return false;
    }
    delay(50);
  }
  if (finger.image2Tz(2) != FINGERPRINT_OK) {
    reportEnrollmentResult(sessionId, false, "Second scan was unclear - try again");
    drawIdleScreen("Scan your finger");
    return false;
  }

  if (finger.createModel() != FINGERPRINT_OK) {
    reportEnrollmentResult(sessionId, false, "The two scans did not match");
    beepError();
    drawIdleScreen("Scan your finger");
    return false;
  }
  if (finger.storeModel(slot) != FINGERPRINT_OK) {
    reportEnrollmentResult(sessionId, false, "Could not store the fingerprint on the sensor");
    beepError();
    drawIdleScreen("Scan your finger");
    return false;
  }

  Serial.printf("Web enrollment SUCCESS: slot %d captured.\n", slot);
  beepOk();
  drawIdleScreen("Captured! Check the admin page");
  reportEnrollmentResult(sessionId, true, "Captured");
  delay(1500);
  drawIdleScreen("Scan your finger");
  return true;
}

void reportEnrollmentResult(long sessionId, bool success, const char* message) {
  if (WiFi.status() != WL_CONNECTED) return;
  HTTPClient http;
  http.setTimeout(6000);
  http.begin(String(BACKEND_BASE_URL) + "/api/v1/scan/enrollment-result");
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Key", DEVICE_API_KEY);

  JsonDocument body;
  body["sessionId"] = sessionId;
  body["success"] = success;
  body["message"] = message;
  String payload;
  serializeJson(body, payload);

  int code = http.POST(payload);
  if (code != 200) Serial.printf("Enrollment result POST HTTP %d\n", code);
  http.end();
}

// ----------------------- serial commands + manual enrollment -----------------
void handleSerialCommands() {
  if (!Serial.available()) return;
  String line = Serial.readStringUntil('\n');
  line.trim();
  if (line.length() == 0) return;

  if (line.startsWith("enroll ")) {
    int slot = line.substring(7).toInt();
    if (slot < 1 || slot > 127) { Serial.println("Slot must be 1..127"); return; }
    enrollFingerprintManual((uint8_t)slot);
  } else if (line.startsWith("delete ")) {
    int slot = line.substring(7).toInt();
    if (slot < 1 || slot > 127) { Serial.println("Slot must be 1..127"); return; }
    Serial.println(finger.deleteModel(slot) == FINGERPRINT_OK ? "Deleted." : "Delete failed.");
  } else if (line == "empty") {
    Serial.println(finger.emptyDatabase() == FINGERPRINT_OK ? "All fingerprints removed." : "Failed.");
  } else if (line == "count") {
    finger.getTemplateCount();
    Serial.printf("%d fingerprint(s) stored.\n", finger.templateCount);
  } else {
    Serial.println("Unknown command. Use: enroll <slot> | delete <slot> | empty | count");
  }
}

// Manual fallback (same two-scan flow as performWebEnrollment(), but talks to Serial only -
// use this only when the admin dashboard/backend is unreachable).
bool enrollFingerprintManual(uint8_t slot) {
  int p = -1;
  drawIdleScreen("Enroll: place finger");
  Serial.printf("Enrolling slot %d - place the finger on the sensor...\n", slot);

  unsigned long start = millis();
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (millis() - start > 20000) { Serial.println("Timed out."); drawIdleScreen("Scan your finger"); return false; }
    delay(50);
  }
  if (finger.image2Tz(1) != FINGERPRINT_OK) { Serial.println("Image 1 failed, try again."); drawIdleScreen("Scan your finger"); return false; }

  Serial.println("Remove finger.");
  drawIdleScreen("Remove finger");
  delay(1500);
  while (finger.getImage() != FINGERPRINT_NOFINGER) delay(50);

  Serial.println("Place the SAME finger again...");
  drawIdleScreen("Place same finger again");
  p = -1;
  start = millis();
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (millis() - start > 20000) { Serial.println("Timed out."); drawIdleScreen("Scan your finger"); return false; }
    delay(50);
  }
  if (finger.image2Tz(2) != FINGERPRINT_OK) { Serial.println("Image 2 failed, try again."); drawIdleScreen("Scan your finger"); return false; }

  if (finger.createModel() != FINGERPRINT_OK) {
    Serial.println("The two scans did not match - enrollment cancelled. Try again.");
    beepError();
    drawIdleScreen("Scan your finger");
    return false;
  }
  if (finger.storeModel(slot) != FINGERPRINT_OK) {
    Serial.println("Could not store the fingerprint.");
    beepError();
    drawIdleScreen("Scan your finger");
    return false;
  }

  Serial.printf("SUCCESS: fingerprint stored in slot %d. Now register the person with slot %d on the People page.\n", slot, slot);
  beepOk();
  drawIdleScreen("Enrolled!");
  delay(1500);
  drawIdleScreen("Scan your finger");
  return true;
}

// -------------------------------- buzzer ------------------------------------
// Distinct beep patterns per outcome, so the coordinator can tell what happened
// without looking at the screen. tone()'s third argument is non-blocking on
// ESP32, so each helper adds its own delay() to actually wait out the note.
void beep(int frequency, int duration) {
  tone(BUZZER_PIN, frequency, duration);
  delay(duration);
  noTone(BUZZER_PIN);
}
void beepReady()           { beep(1800, 150); delay(80); beep(2500, 250); }  // sensor found at boot
void beepScanning()        { beep(3000, 100); }                              // finger detected, capturing
void beepOk()               { beep(2500, 120); delay(80); beep(3200, 120); delay(80); beep(4000, 250); }
void beepDuplicate()      { beep(1500, 120); delay(200); beep(1500, 120); }
void beepNotRecognized()  { beep(800, 300); }
void beepError()          { beep(800, 300); delay(100); beep(800, 300); }

// --------------------------- TFT display (stubs) ----------------------------
// Everything shows on the Serial Monitor until you add your TFT library here.
// The 3.5" shield (480x320) is an 8-bit parallel panel (typically ILI9486/ILI9488).
// With TFT_eSPI: select the driver + ESP32 parallel setup in its User_Setup.h using the
// TFT_* pin defines above (TFT_RST, TFT_CS, TFT_RS, TFT_WR, TFT_RD, TFT_D0..D7) - see
// esp32/User_Setup_BCI_ILI9488.h in this repo, and the standalone esp32/hardware-test/
// sketch for bringing the sensor + buzzer + screen up together before flashing this one.
void initDisplay() {
  tft.init();
  tft.setRotation(1);                 // landscape; use 3 to flip 180 degrees if upside down
  tft.fillScreen(TFT_BLACK);
  tft.setTextDatum(MC_DATUM);
}

void drawIdleScreen(const char* prompt) {
  Serial.println(String("[LCD] ") + prompt);
  tft.fillScreen(TFT_BLACK);
  tft.setTextColor(TFT_CYAN, TFT_BLACK);
  tft.setTextSize(3);
  tft.drawString(prompt, tft.width() / 2, tft.height() / 2);
}

void drawStatusScreen(const char* line1, const char* line2, bool success) {
  Serial.println(String("[LCD] ") + line1 + " / " + line2);
  uint16_t color = success ? TFT_GREEN : TFT_RED;
  tft.fillScreen(TFT_BLACK);
  tft.setTextColor(color, TFT_BLACK);
  tft.setTextSize(3);
  tft.drawString(line1, tft.width() / 2, tft.height() / 2 - 25);
  tft.setTextSize(2);
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.drawString(line2, tft.width() / 2, tft.height() / 2 + 15);
}
