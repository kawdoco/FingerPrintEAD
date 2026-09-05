/*
 * fingerprint_functions.cpp
 * BCI Attendance System - R307S Fingerprint Sensor
 *
 * Requires: Adafruit Fingerprint Sensor Library
 *   Arduino IDE -> Tools -> Manage Libraries -> "Adafruit Fingerprint Sensor Library"
 */

#include <Adafruit_Fingerprint.h>
#include "config.h"
#include "fingerprint_functions.h"

static HardwareSerial fingerSerial(2); // ESP32 UART2
static Adafruit_Fingerprint finger(&fingerSerial);

bool fingerprintInit() {
  fingerSerial.begin(FINGERPRINT_BAUD, SERIAL_8N1, FINGERPRINT_RX_PIN, FINGERPRINT_TX_PIN);
  delay(200);

  if (!finger.verifyPassword()) {
    Serial.println("[Fingerprint] ERROR: sensor not found / wrong baud rate / wiring issue.");
    return false;
  }

  finger.getParameters();
  Serial.print("[Fingerprint] Sensor OK. Capacity: ");
  Serial.println(finger.capacity);

  finger.getTemplateCount();
  Serial.print("[Fingerprint] Templates stored: ");
  Serial.println(finger.templateCount);

  return true;
}

uint16_t fingerprintTemplateCount() {
  finger.getTemplateCount();
  return finger.templateCount;
}

FingerprintResult fingerprintScanAndVerify(uint16_t &matchedSlotId, uint16_t &matchConfidence) {
  int p = finger.getImage();

  if (p == FINGERPRINT_NOFINGER) {
    return FingerprintResult::NO_FINGER;
  }
  if (p != FINGERPRINT_OK) {
    return FingerprintResult::IMAGE_FAIL;
  }

  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) {
    return FingerprintResult::IMAGE_FAIL;
  }

  p = finger.fingerFastSearch();
  if (p != FINGERPRINT_OK) {
    return FingerprintResult::NO_MATCH;
  }

  matchedSlotId = finger.fingerID;
  matchConfidence = finger.confidence;
  return FingerprintResult::OK;
}

FingerprintResult fingerprintEnroll(uint16_t slotId) {
  if (slotId < FINGERPRINT_MIN_SLOT || slotId > FINGERPRINT_MAX_SLOT) {
    return FingerprintResult::INVALID_SLOT;
  }

  int p = -1;

  // --- First capture ---
  Serial.println("[Fingerprint] Place finger to enroll...");
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
      continue;
    }
    if (p != FINGERPRINT_OK) return FingerprintResult::IMAGE_FAIL;
  }

  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) return FingerprintResult::IMAGE_FAIL;

  Serial.println("[Fingerprint] Remove finger...");
  delay(1500);
  p = 0;
  while (p != FINGERPRINT_NOFINGER) {
    p = finger.getImage();
    delay(50);
  }

  // --- Second capture (confirmation) ---
  Serial.println("[Fingerprint] Place same finger again...");
  p = -1;
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
      continue;
    }
    if (p != FINGERPRINT_OK) return FingerprintResult::IMAGE_FAIL;
  }

  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) return FingerprintResult::IMAGE_FAIL;

  p = finger.createModel();
  if (p != FINGERPRINT_OK) {
    Serial.println("[Fingerprint] Fingers did not match.");
    return FingerprintResult::ENROLL_FAIL;
  }

  p = finger.storeModel(slotId);
  if (p != FINGERPRINT_OK) {
    Serial.println("[Fingerprint] Could not store model.");
    return FingerprintResult::ENROLL_FAIL;
  }

  Serial.print("[Fingerprint] Enrolled successfully at slot ");
  Serial.println(slotId);
  return FingerprintResult::OK;
}

FingerprintResult fingerprintDelete(uint16_t slotId) {
  if (slotId < FINGERPRINT_MIN_SLOT || slotId > FINGERPRINT_MAX_SLOT) {
    return FingerprintResult::INVALID_SLOT;
  }

  int p = finger.deleteModel(slotId);
  if (p != FINGERPRINT_OK) {
    return FingerprintResult::SENSOR_ERROR;
  }
  return FingerprintResult::OK;
}
