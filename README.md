# 🧠 BCI Research Lab Smart Attendance System

## 📌 Project Overview

The **BCI Research Lab Smart Attendance System** is an IoT-based fingerprint attendance and laboratory management system designed to automate attendance tracking using biometric authentication.

The system uses an **R307S Fingerprint Sensor** connected to an **ESP32**. When a fingerprint is recognized, the ESP32 communicates through **Wi-Fi** with a backend server. The attendance information is stored in a database and displayed through a complete **BCI Research Lab Web Dashboard**.

## 🔄 System Flow

```text
Fingerprint Scanner
        ↓
      ESP32
        ↓
      Wi-Fi
        ↓
   Backend / API
        ↓
     Database
        ↓
BCI Research Lab Web UI