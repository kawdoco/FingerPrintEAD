/*
 * config.h
 * BCI Attendance System - Firmware Configuration
 * Owner: Dinith (Hardware + Firmware + Device Integration)
 *
 * Edit the values below to match your hardware wiring, Wi-Fi network,
 * and the Spring Boot backend that Gimsara is building.
 */

#ifndef CONFIG_H
#define CONFIG_H

// ================== DEVICE IDENTITY ==================
// Must match the deviceId Gimsara registers in the Devices table on the backend.
#define DEVICE_ID          "BCI_ESP32_001"
#define DEVICE_NAME        "Main Entrance Scanner"
#define FIRMWARE_VERSION   "1.0.0"

// ================== WI-FI CREDENTIALS ==================
#define WIFI_SSID          "YOUR_WIFI_SSID"
#define WIFI_PASSWORD      "YOUR_WIFI_PASSWORD"
#define WIFI_CONNECT_TIMEOUT_MS   15000
#define WIFI_RECONNECT_INTERVAL_MS 10000

// ================== BACKEND (Spring Boot API) ==================
// Ask Gimsara for the deployed/local IP + port of the Spring Boot server.
// Example local:      "http://192.168.1.50:8080"
// Example deployed:   "https://bci-backend.example.com"
#define SERVER_BASE_URL         "http://192.168.1.50:8080"
#define ATTENDANCE_ENDPOINT     SERVER_BASE_URL "/api/attendance"
#define DEVICE_HEARTBEAT_ENDPOINT SERVER_BASE_URL "/api/devices/heartbeat"
#define HTTP_TIMEOUT_MS          8000
#define HEARTBEAT_INTERVAL_MS    60000UL   // send a "device is alive" ping every 60s

// ================== NTP TIME SYNC ==================
#define NTP_SERVER_1       "pool.ntp.org"
#define NTP_SERVER_2       "time.nist.gov"
// Sri Lanka Standard Time = UTC+5:30
#define GMT_OFFSET_SEC      (5 * 3600 + 1800)
#define DAYLIGHT_OFFSET_SEC 0

// ================== R307S FINGERPRINT SENSOR (UART) ==================
// R307S uses HardwareSerial2 on the ESP32.
#define FINGERPRINT_RX_PIN   16   // ESP32 RX2 <- R307S TX
#define FINGERPRINT_TX_PIN   17   // ESP32 TX2 -> R307S RX
#define FINGERPRINT_BAUD     57600

// ================== TFT DISPLAY (3.5" SPI, via TFT_eSPI) ==================
// NOTE: The actual pin mapping for TFT_eSPI is set in the library's
// User_Setup.h (or User_Setup_Select.h), NOT here, because TFT_eSPI reads
// pins at compile time. The values below are for reference / wiring only
// and must match what you configure in User_Setup.h.
#define TFT_PIN_MOSI   23
#define TFT_PIN_MISO   19
#define TFT_PIN_SCLK   18
#define TFT_PIN_CS      5
#define TFT_PIN_DC      2
#define TFT_PIN_RST     4
#define TFT_PIN_BL     32   // backlight (optional, PWM capable)

// Touch controller (resistive/XPT2046 style) chip-select pin
#define TOUCH_CS_PIN   15

// ================== BUZZER ==================
#define BUZZER_PIN     25
#define BEEP_SUCCESS_MS   150   // short beep
#define BEEP_ERROR_MS     600   // long beep

// ================== ENROLLMENT ==================
// Valid fingerprint slot ID range accepted by the R307S module.
#define FINGERPRINT_MIN_SLOT   1
#define FINGERPRINT_MAX_SLOT   162

#endif // CONFIG_H
