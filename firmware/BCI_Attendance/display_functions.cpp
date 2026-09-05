/*
 * display_functions.cpp
 * BCI Attendance System - TFT + Touch screens
 *
 * Requires: TFT_eSPI library (Bodmer)
 *   Arduino IDE -> Tools -> Manage Libraries -> "TFT_eSPI"
 *   IMPORTANT: pin mapping must be set in the library's User_Setup.h,
 *   matching the pins documented in config.h. This file assumes that
 *   setup is done and just draws to the screen.
 */

#include <TFT_eSPI.h>
#include <SPI.h>
#include "config.h"
#include "display_functions.h"

static TFT_eSPI tft = TFT_eSPI();

// Simple calibration data placeholder. Real values should be produced by
// touchCalibrate() and then hard-coded here (or stored in EEPROM/NVS) once
// Dinith calibrates the physical unit.
static uint16_t calData[5] = {275, 3620, 264, 3532, 1};
static bool calibrated = false;

void displayInit() {
  tft.init();
  tft.setRotation(1); // landscape; adjust 0-3 to match physical mounting
  tft.fillScreen(TFT_BLACK);

#ifdef TFT_PIN_BL
  pinMode(TFT_PIN_BL, OUTPUT);
  digitalWrite(TFT_PIN_BL, HIGH); // backlight on
#endif

  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setTextSize(2);
}

// ---------------- Screens ----------------

void screenWelcome() {
  tft.fillScreen(TFT_BLACK);
  tft.setTextSize(3);
  tft.setTextColor(TFT_CYAN, TFT_BLACK);
  tft.setCursor(20, 40);
  tft.println("BCI Attendance");

  tft.setTextSize(2);
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setCursor(20, 90);
  tft.println("Device: " DEVICE_NAME);

  tft.setCursor(20, 140);
  tft.println("Touch to scan finger");
}

void screenScanFingerprint() {
  tft.fillScreen(TFT_BLACK);
  tft.setTextSize(2);
  tft.setTextColor(TFT_YELLOW, TFT_BLACK);
  tft.setCursor(20, 60);
  tft.println("Place your finger");
  tft.setCursor(20, 90);
  tft.println("on the sensor...");
}

void screenEnrollment(const String &step) {
  tft.fillScreen(TFT_BLACK);
  tft.setTextSize(2);
  tft.setTextColor(TFT_ORANGE, TFT_BLACK);
  tft.setCursor(20, 40);
  tft.println("Enrollment Mode");

  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setCursor(20, 90);
  tft.println(step);
}

void screenSuccess(const String &name) {
  tft.fillScreen(TFT_BLACK);
  tft.setTextSize(3);
  tft.setTextColor(TFT_GREEN, TFT_BLACK);
  tft.setCursor(20, 60);
  tft.println("Welcome!");

  tft.setTextSize(2);
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setCursor(20, 110);
  tft.println(name);

  tft.setCursor(20, 140);
  tft.println("Attendance marked.");
}

void screenError(const String &message) {
  tft.fillScreen(TFT_BLACK);
  tft.setTextSize(3);
  tft.setTextColor(TFT_RED, TFT_BLACK);
  tft.setCursor(20, 60);
  tft.println("Error");

  tft.setTextSize(2);
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setCursor(20, 110);
  tft.println(message);
}

void screenWifiStatus(bool connected, const String &ipOrMessage) {
  tft.fillScreen(TFT_BLACK);
  tft.setTextSize(2);
  tft.setTextColor(connected ? TFT_GREEN : TFT_RED, TFT_BLACK);
  tft.setCursor(20, 40);
  tft.println(connected ? "Wi-Fi Connected" : "Wi-Fi Disconnected");

  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setCursor(20, 80);
  tft.println(ipOrMessage);
}

// ---------------- Touch ----------------

bool touchInit() {
  // TFT_eSPI handles the touch controller internally when configured with
  // TOUCH_CS in User_Setup.h. We just load calibration data here.
  tft.setTouch(calData);
  calibrated = true;
  return true;
}

bool touchGetPoint(uint16_t &x, uint16_t &y) {
  if (!calibrated) return false;
  return tft.getTouch(&x, &y);
}

void touchCalibrate() {
  tft.fillScreen(TFT_BLACK);
  tft.setCursor(10, 10);
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setTextSize(2);
  tft.println("Touch the corners");
  tft.println("as prompted...");

  // TFT_eSPI's built-in calibration routine; prints calData values to
  // Serial and to calData[] so they can be copied into the array above.
  tft.calibrateTouch(calData, TFT_WHITE, TFT_BLACK, 15);

  Serial.println("[Touch] Calibration data (copy into calData[]):");
  for (int i = 0; i < 5; i++) {
    Serial.print(calData[i]);
    if (i < 4) Serial.print(", ");
  }
  Serial.println();

  calibrated = true;
}
