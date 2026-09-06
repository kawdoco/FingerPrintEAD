/*
 * BCI_Attendance.ino
 * BCI Attendance System - Main Firmware
 * Owner: Dinith (Hardware Lead + Firmware + Device Integration)
 *
 * Flow (per workload plan):
 *   Fingerprint -> ESP32 -> TFT Message -> Wi-Fi -> Spring Boot
 *
 * Required libraries (Arduino IDE -> Manage Libraries):
 *   - Adafruit Fingerprint Sensor Library
 *   - TFT_eSPI (Bodmer)
 *   - (WiFi, HTTPClient, time.h are bundled with the ESP32 board package)
 *
 * See README.md for wiring, TFT_eSPI configuration, and the
 * hardware/firmware testing checklists.
 */

#include <Arduino.h>
#include "config.h"
#include "buzzer_functions.h"
#include "fingerprint_functions.h"
#include "display_functions.h"
#include "network_functions.h"

// ---------------- State machine ----------------
enum class AppState {
  BOOTING,
  WELCOME,
  SCANNING,
  SENDING,
  SUCCESS,
  ERROR_STATE,
  ENROLLING
};

static AppState state = AppState::BOOTING;
static unsigned long stateEnteredAt = 0;
static unsigned long lastHeartbeat = 0;
static String lastErrorMessage = "";
static String lastSuccessLabel = "";

// Touch zone (top-right corner) used to enter enrollment mode for demos/testing.
// Adjust to your screen resolution/rotation if needed.
static const uint16_t ENROLL_ZONE_X_MIN = 260;
static const uint16_t ENROLL_ZONE_Y_MAX = 40;

static void enterState(AppState newState) {
  state = newState;
  stateEnteredAt = millis();
}

// ---------------- Setup ----------------

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println("\n[BCI Attendance] Booting firmware v" FIRMWARE_VERSION);

  buzzerInit();
  displayInit();
  touchInit();

  screenWifiStatus(false, "Connecting...");
  bool wifiOk = wifiConnect();
  if (wifiOk) {
    timeSync();
    screenWifiStatus(true, wifiLocalIP());
  } else {
    screenWifiStatus(false, "Retrying in background");
  }
  delay(1000);

  if (!fingerprintInit()) {
    screenError("Fingerprint sensor\nnot detected!");
    buzzerErrorBeep();
    // Keep running so Wi-Fi/heartbeat still work and the issue is visible
    // on the TFT; hardware team can fix wiring and reset.
    delay(3000);
  }

  enterState(AppState::WELCOME);
}

// ---------------- Helpers ----------------

static bool touchInEnrollZone() {
  uint16_t x, y;
  if (!touchGetPoint(x, y)) return false;
  return (x >= ENROLL_ZONE_X_MIN && y <= ENROLL_ZONE_Y_MAX);
}

static void handleEnrollmentFlow() {
  screenEnrollment("Starting enrollment...");
  delay(800);

  uint16_t nextSlot = fingerprintTemplateCount() + 1;
  if (nextSlot > FINGERPRINT_MAX_SLOT) nextSlot = FINGERPRINT_MIN_SLOT;

  screenEnrollment("Place finger (slot " + String(nextSlot) + ")");
  FingerprintResult result = fingerprintEnroll(nextSlot);

  if (result == FingerprintResult::OK) {
    screenEnrollment("Enrolled! Slot " + String(nextSlot));
    buzzerDoubleBeep();
    delay(1500);
  } else {
    screenError("Enrollment failed.\nTry again.");
    buzzerErrorBeep();
    delay(1500);
  }

  enterState(AppState::WELCOME);
}

// ---------------- Loop ----------------

void loop() {
  wifiMaintain();

  // Periodic heartbeat so the dashboard shows this device as online.
  if (millis() - lastHeartbeat > HEARTBEAT_INTERVAL_MS) {
    lastHeartbeat = millis();
    sendHeartbeat();
  }

  switch (state) {

    case AppState::WELCOME: {
      screenWelcome();
      // Special corner touch -> enrollment mode (for setup/testing by Dinith).
      if (touchInEnrollZone()) {
        enterState(AppState::ENROLLING);
        break;
      }
      enterState(AppState::SCANNING);
      break;
    }

    case AppState::SCANNING: {
      screenScanFingerprint();
      uint16_t slotId = 0, confidence = 0;

      // Poll the sensor for up to ~8 seconds before returning to welcome.
      unsigned long scanStart = millis();
      FingerprintResult result = FingerprintResult::NO_FINGER;

      while (millis() - scanStart < 8000) {
        result = fingerprintScanAndVerify(slotId, confidence);
        if (result != FingerprintResult::NO_FINGER) break;

        if (touchInEnrollZone()) {
          enterState(AppState::ENROLLING);
          return;
        }
        delay(100);
      }

      if (result == FingerprintResult::OK) {
        lastSuccessLabel = "Fingerprint ID " + String(slotId);
        enterState(AppState::SENDING);

        // Send to backend, then show result.
        buzzerSuccessBeep();
        bool sent = sendAttendance(slotId);
        if (sent) {
          enterState(AppState::SUCCESS);
        } else {
          lastErrorMessage = "Could not reach\nserver. Saved locally?"; // TODO: local queue/retry
          enterState(AppState::ERROR_STATE);
        }
      } else if (result == FingerprintResult::NO_MATCH) {
        lastErrorMessage = "Fingerprint not\nrecognized.";
        buzzerErrorBeep();
        enterState(AppState::ERROR_STATE);
      } else {
        // Timed out waiting (NO_FINGER for 8s) -> just go back to welcome quietly.
        enterState(AppState::WELCOME);
      }
      break;
    }

    case AppState::SENDING: {
      // Transitional state; handled synchronously above. Kept for clarity
      // in case this is later made non-blocking / queued.
      enterState(AppState::WELCOME);
      break;
    }

    case AppState::SUCCESS: {
      screenSuccess(lastSuccessLabel);
      if (millis() - stateEnteredAt > 2500) {
        enterState(AppState::WELCOME);
      }
      break;
    }

    case AppState::ERROR_STATE: {
      screenError(lastErrorMessage);
      if (millis() - stateEnteredAt > 2500) {
        enterState(AppState::WELCOME);
      }
      break;
    }

    case AppState::ENROLLING: {
      handleEnrollmentFlow();
      break;
    }

    default:
      enterState(AppState::WELCOME);
      break;
  }
}
