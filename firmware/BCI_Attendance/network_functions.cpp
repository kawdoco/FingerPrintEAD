/*
 * network_functions.cpp
 * BCI Attendance System - Wi-Fi connection + Spring Boot communication
 *
 * Requires: WiFi.h and HTTPClient.h (bundled with the ESP32 Arduino core).
 * IMPORTANT: The JSON keys sent here (deviceId, fingerprintId, timestamp)
 * must exactly match what Gimsara's AttendanceController expects on the
 * backend, per the shared constants rule in the project notes.
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <time.h>
#include "config.h"
#include "network_functions.h"

static unsigned long lastReconnectAttempt = 0;

bool wifiConnect() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("[WiFi] Connecting to ");
  Serial.print(WIFI_SSID);

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - start > WIFI_CONNECT_TIMEOUT_MS) {
      Serial.println("\n[WiFi] Connection timed out.");
      return false;
    }
    delay(300);
    Serial.print(".");
  }

  Serial.println();
  Serial.print("[WiFi] Connected. IP: ");
  Serial.println(WiFi.localIP());
  return true;
}

void wifiMaintain() {
  if (WiFi.status() == WL_CONNECTED) return;

  unsigned long now = millis();
  if (now - lastReconnectAttempt < WIFI_RECONNECT_INTERVAL_MS) return;
  lastReconnectAttempt = now;

  Serial.println("[WiFi] Connection lost. Reconnecting...");
  WiFi.disconnect();
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

bool wifiIsConnected() {
  return WiFi.status() == WL_CONNECTED;
}

String wifiLocalIP() {
  return WiFi.localIP().toString();
}

bool timeSync() {
  configTime(GMT_OFFSET_SEC, DAYLIGHT_OFFSET_SEC, NTP_SERVER_1, NTP_SERVER_2);

  struct tm timeinfo;
  int attempts = 0;
  while (!getLocalTime(&timeinfo) && attempts < 10) {
    Serial.println("[NTP] Waiting for time sync...");
    delay(500);
    attempts++;
  }

  if (attempts >= 10) {
    Serial.println("[NTP] Time sync failed.");
    return false;
  }

  Serial.println("[NTP] Time synced.");
  return true;
}

String currentTimestampISO() {
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    return "1970-01-01T00:00:00"; // fallback if NTP never synced
  }

  char buf[25];
  strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%S", &timeinfo);
  return String(buf);
}

static bool postJson(const char *url, const String &jsonBody) {
  if (!wifiIsConnected()) {
    Serial.println("[HTTP] Skipped: no Wi-Fi connection.");
    return false;
  }

  HTTPClient http;
  http.setTimeout(HTTP_TIMEOUT_MS);
  http.begin(url);
  http.addHeader("Content-Type", "application/json");

  int httpCode = http.POST(jsonBody);

  bool ok = (httpCode >= 200 && httpCode < 300);
  Serial.print("[HTTP] POST ");
  Serial.print(url);
  Serial.print(" -> ");
  Serial.println(httpCode);

  if (!ok) {
    String response = http.getString();
    Serial.print("[HTTP] Response body: ");
    Serial.println(response);
  }

  http.end();
  return ok;
}

bool sendAttendance(uint16_t fingerprintId) {
  String timestamp = currentTimestampISO();

  // JSON shape must match the example in the workload plan exactly:
  // { "deviceId": "BCI_ESP32_001", "fingerprintId": 12, "timestamp": "..." }
  String json = "{";
  json += "\"deviceId\":\"" DEVICE_ID "\",";
  json += "\"fingerprintId\":" + String(fingerprintId) + ",";
  json += "\"timestamp\":\"" + timestamp + "\"";
  json += "}";

  Serial.print("[Attendance] Sending: ");
  Serial.println(json);

  return postJson(ATTENDANCE_ENDPOINT, json);
}

bool sendHeartbeat() {
  String json = "{";
  json += "\"deviceId\":\"" DEVICE_ID "\",";
  json += "\"status\":\"ONLINE\",";
  json += "\"firmwareVersion\":\"" FIRMWARE_VERSION "\"";
  json += "}";

  return postJson(DEVICE_HEARTBEAT_ENDPOINT, json);
}
