# BCI Research Lab — Fingerprint Attendance System

A campus attendance system for the BCI Research Lab: students, lecturers, Rev. Fathers and
guests all check in with a fingerprint at the lab door, and a live dashboard shows who's in.

```
Finger → R307S sensor → ESP32 → Spring Boot API → Supabase (Postgres)
                                        │
                                        └──→ React dashboard (live feed, light/dark theme)
```

## Stack

| Layer     | Technology                                              |
|-----------|----------------------------------------------------------|
| Frontend  | React (Vite) + Tailwind CSS (light theme by default, dark toggle) |
| Backend   | Java 17, Spring Boot 3 (MVC: Controller → Service → Repository → Model) |
| Database  | Supabase (managed Postgres)                               |
| Auth      | JWT for admin dashboard, API-key header for devices |
| Device    | ESP32 + R307S fingerprint sensor + 3.5" TFT shield + buzzer |

## Who uses it

Four kinds of people can be registered and scan in: **students**, **lecturers**, **Rev.
Fathers** and **guests**. There's no late/absent/leave tracking — just check-in, check-out,
and the day's totals.

## Repository layout

```
bci-attendance-system/
├── backend/          Spring Boot API (Maven project)
├── frontend/          React app — public live display + admin dashboard
├── database/          Supabase SQL: schema, RLS policies
├── esp32/              Arduino/ESP32 firmware for the fingerprint kiosk
└── docs/               Architecture, API reference, setup guide
```

## Quick start

**Fastest way** (demo mode, one command; needs JDK 17+, Maven, Node 18+):
```powershell
.\start.ps1          # Windows        (macOS/Linux: ./start.sh)
```
It starts the backend and the frontend, opens the live display, and prints the URL to put in the ESP32 firmware.
Hardware wiring and flashing: [docs/ESP32_INTEGRATION.md](docs/ESP32_INTEGRATION.md).

**Try it without any setup** (in-memory database, 8 demo people across all four types):
```powershell
cd backend ; .\run-backend.ps1 -Local          # terminal 1
cd frontend ; npm install ; npm run dev        # terminal 2
```
Open http://localhost:5173/ (live display) and http://localhost:5173/admin/login (`admin` / `ChangeMe123!`).

**With Supabase:** copy `backend/.env.example` to `backend/.env`, fill in the *Session pooler*
details, then `.\run-backend.ps1`. Full walkthrough incl. kiosk wiring and registering people:
[docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md).

## Registering people

From the admin **People** page, click **Scan finger** — no Serial Monitor typing needed. The
kiosk prompts for a two-touch scan, reports back, and the page shows a form to fill in the
person's name, type and details. See [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md#step-4---register-people).

## Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — system design, MVC layering, data flow, the "scan finger" workflow
- [docs/API_REFERENCE.md](docs/API_REFERENCE.md) — every REST endpoint
- [docs/DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md) — tables, relationships, biometric-privacy notes
- [database/schema.sql](database/schema.sql) — source-of-truth SQL
- [docs/ESP32_INTEGRATION.md](docs/ESP32_INTEGRATION.md) — wiring (matches the lab's diagram), firmware, enrollment workflow
- [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md) — step-by-step local + campus deployment
