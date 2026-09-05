/*
 * display_functions.h
 * BCI Attendance System - TFT + Touch screens (Steps 4 & 5/6 of firmware plan)
 *
 * Requires: TFT_eSPI library, configured for your specific 3.5" display
 * via the library's User_Setup.h (see README.md "TFT_eSPI setup" section).
 */

#ifndef DISPLAY_FUNCTIONS_H
#define DISPLAY_FUNCTIONS_H

#include <Arduino.h>

void displayInit();

// --- Screens (Step 4) ---
void screenWelcome();
void screenScanFingerprint();
void screenEnrollment(const String &step);   // e.g. "Place finger", "Remove finger", "Place again"
void screenSuccess(const String &name);
void screenError(const String &message);
void screenWifiStatus(bool connected, const String &ipOrMessage);

// --- Touch (Step 6) ---
bool touchInit();
// Returns true if a touch point was read this call, fills x/y with screen coords.
bool touchGetPoint(uint16_t &x, uint16_t &y);
// Interactive calibration routine (run once, or when TOUCH_CALIBRATE is requested).
void touchCalibrate();

#endif // DISPLAY_FUNCTIONS_H
