/*
 * network_functions.h
 * BCI Attendance System - Wi-Fi connection + Spring Boot communication
 * (Steps 6 & 7 of firmware plan)
 */

#ifndef NETWORK_FUNCTIONS_H
#define NETWORK_FUNCTIONS_H

#include <Arduino.h>

// Connects to Wi-Fi using credentials from config.h.
// Blocks (with timeout) until connected or WIFI_CONNECT_TIMEOUT_MS elapses.
bool wifiConnect();

// Call from loop() periodically; reconnects automatically if the
// connection drops. Non-blocking-ish (uses WIFI_RECONNECT_INTERVAL_MS).
void wifiMaintain();

bool wifiIsConnected();
String wifiLocalIP();

// Synchronizes the ESP32's internal clock via NTP. Call once after Wi-Fi connects.
bool timeSync();

// Returns current timestamp formatted as ISO-8601, e.g. "2026-09-04T10:30:00"
// matching the JSON example in the workload plan.
String currentTimestampISO();

// Sends an attendance event to the Spring Boot backend.
// POST /api/attendance  { "deviceId", "fingerprintId", "timestamp" }
// Returns true on HTTP 2xx response.
bool sendAttendance(uint16_t fingerprintId);

// Sends a lightweight heartbeat so the backend's Device Monitoring page
// (Thamashi's dashboard) can show Online/Offline + Last Seen.
bool sendHeartbeat();

#endif // NETWORK_FUNCTIONS_H
