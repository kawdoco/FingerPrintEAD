/*
 * buzzer_functions.h
 * BCI Attendance System - Buzzer Feedback (Step 5 of firmware plan)
 */

#ifndef BUZZER_FUNCTIONS_H
#define BUZZER_FUNCTIONS_H

void buzzerInit();
void buzzerSuccessBeep();   // short beep -> success
void buzzerErrorBeep();     // long beep  -> error
void buzzerDoubleBeep();    // used for "enrollment complete"

#endif // BUZZER_FUNCTIONS_H
