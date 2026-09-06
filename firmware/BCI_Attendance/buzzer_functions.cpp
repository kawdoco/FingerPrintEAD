/*
 * buzzer_functions.cpp
 * BCI Attendance System - Buzzer Feedback
 */

#include <Arduino.h>
#include "config.h"
#include "buzzer_functions.h"

void buzzerInit() {
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
}

void buzzerSuccessBeep() {
  digitalWrite(BUZZER_PIN, HIGH);
  delay(BEEP_SUCCESS_MS);
  digitalWrite(BUZZER_PIN, LOW);
}

void buzzerErrorBeep() {
  digitalWrite(BUZZER_PIN, HIGH);
  delay(BEEP_ERROR_MS);
  digitalWrite(BUZZER_PIN, LOW);
}

void buzzerDoubleBeep() {
  for (int i = 0; i < 2; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(100);
    digitalWrite(BUZZER_PIN, LOW);
    delay(100);
  }
}
