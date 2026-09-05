/*
 * fingerprint_functions.h
 * BCI Attendance System - R307S Fingerprint Sensor (Step 3 of firmware plan)
 *
 * Wraps the Adafruit Fingerprint Sensor Library with the 5 operations
 * required by the workload plan: init, enroll, scan, verify, delete.
 */

#ifndef FINGERPRINT_FUNCTIONS_H
#define FINGERPRINT_FUNCTIONS_H

#include <Arduino.h>

// Result codes returned to the main state machine.
enum class FingerprintResult {
  OK,
  NO_FINGER,          // no finger present on sensor (not an error, just "keep waiting")
  IMAGE_FAIL,
  NO_MATCH,            // scanned finger doesn't match any enrolled template
  ENROLL_FAIL,
  SENSOR_ERROR,
  INVALID_SLOT
};

// Must be called once in setup() before any other fingerprint function.
bool fingerprintInit();

// Blocking-ish scan: call repeatedly from loop(). Returns immediately with
// NO_FINGER if nothing is on the sensor yet (non-blocking poll pattern).
// On OK, matchedSlotId is filled with the enrolled fingerprint ID (student's fingerprint_id).
FingerprintResult fingerprintScanAndVerify(uint16_t &matchedSlotId, uint16_t &matchConfidence);

// Enrolls a new fingerprint into the given slot (1..FINGERPRINT_MAX_SLOT).
// This is interactive: it will prompt via the TFT (through callbacks) for
// "place finger" -> "remove finger" -> "place same finger again".
FingerprintResult fingerprintEnroll(uint16_t slotId);

// Deletes a previously enrolled fingerprint template from a slot.
FingerprintResult fingerprintDelete(uint16_t slotId);

// Returns how many templates are currently stored on the sensor.
uint16_t fingerprintTemplateCount();

#endif // FINGERPRINT_FUNCTIONS_H
