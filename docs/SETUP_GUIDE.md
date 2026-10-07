# Setup Guide (Windows-friendly)

You need: **JDK 17+**, **Maven**, **Node 18+**. Arduino IDE is only needed for the kiosk.

## Step 0 - Prove the whole system works (2 minutes, no Supabase)

```powershell
# Terminal 1 - backend with an in-memory database and 8 demo people
# (5 students, a lecturer, a Rev. Father and a guest)
cd backend
.\run-backend.ps1 -Local

# Terminal 2 - frontend
cd frontend
npm install
npm run dev
```

* Live display: http://localhost:5173/ (use the sun/moon button top-right to switch light/dark)
* Admin: http://localhost:5173/admin/login -> `admin` / `ChangeMe123!`
* Simulate a fingerprint scan (PowerShell, no hardware needed):

```powershell
curl.exe -X POST http://localhost:8080/api/v1/scan `
  -H "X-Device-Key: local-demo-device-key" -H "Content-Type: application/json" `
  -d '{\"fingerprintTemplateId\": 1}'
```

The live display updates instantly. Scan slot 1 again after 2 minutes to see the check-out.
Data is lost when you stop the backend - this mode is only for testing.

## Step 1 - Connect Supabase (this is what failed before)

The error `UnknownHostException: db.<ref>.supabase.co` happens because Supabase's *direct*
host is **IPv6-only**, and most home/campus networks have no IPv6. Use the **Session pooler**:

1. Supabase dashboard -> your project -> click **Connect** (top bar).
2. Choose **Session pooler**. Note the host (`aws-0-<region>.pooler.supabase.com`), port
   `5432` and user (`postgres.<project-ref>` - note the dot and project ref).
3. Copy `backend\.env.example` to `backend\.env` and fill in the URL, user and password.
4. (Optional) In the Supabase **SQL Editor** run `database/schema.sql`. If you skip this the
   backend creates the tables automatically on first start (`ddl-auto: update`).
5. If the project shows *Paused* in the dashboard, click **Restore project** first.
6. Forgot the database password? Project Settings -> Database -> **Reset database password**.

Then:

```powershell
cd backend
.\run-backend.ps1
```

On the first start with empty tables the app creates:
* an admin login (`APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`),
* the first kiosk `LAB204-R307S-01` and prints its **API key** in the console
  (`Created first device ... API key: ...`) - copy it for the firmware.

Health check: http://localhost:8080/actuator/health -> `{"status":"UP"}`

> Security: never commit `backend/.env`, and never paste your database password into chats or
> screenshots. If it was exposed, reset it in Supabase.

## Step 2 - Frontend

```powershell
cd frontend
npm install
npm run dev
```

Dev mode proxies `/api` and `/ws` to the backend, so `.env` can stay empty.
Put http://localhost:5173/ in full-screen (F11) on the lab TV. The theme (light by default)
is saved per browser, so the TV and the admin's laptop can each keep their own.

Production: `npm run build`, serve `frontend/dist` with any static host, set
`VITE_API_BASE_URL` / `VITE_WS_URL` before building, and add that site's URL to
`APP_CORS_ORIGINS` on the backend.

## Step 3 - Kiosk hardware

1. Wire everything as in `docs/ESP32_INTEGRATION.md` — 5V for the R307S sensor and buzzer,
   3.3V (from the LM2596 buck converter) for the ESP32 and the TFT shield.
2. Install **TFT_eSPI** (Library Manager) and configure it for this shield using
   `esp32/User_Setup_BCI_ILI9488.h` — see that file's header comment. This has to happen before
   anything else compiles correctly against the screen.
3. **Test the hardware together first**: flash `esp32/hardware-test/hardware_integration_test.ino`
   (no WiFi, no backend) and confirm the sensor, buzzer and screen all work — see
   `esp32/hardware-test/README.md`.
4. Open `esp32/fingerprint_attendance.ino`, set `WIFI_SSID`, `WIFI_PASSWORD`,
   `BACKEND_BASE_URL` (the PC's LAN IP from `ipconfig`, e.g. `http://192.168.1.50:8080`) and
   `DEVICE_API_KEY`. Install the libraries *Adafruit Fingerprint Sensor Library* and *ArduinoJson*.
5. Windows Firewall must allow inbound TCP 8080 on the PC running the backend, and the ESP32
   and the PC must be on the same network.
6. Flash, open the Serial Monitor (115200 baud, "Newline").

## Step 4 - Register people

The normal way is entirely from the browser, no typing on the kiosk required:

1. Admin -> **People** -> pick the kiosk (if you have more than one) -> click **Scan finger**.
2. Have the person place a finger on the sensor, then the same finger again when prompted.
3. The page shows "Fingerprint captured in slot N" - fill in their name, type (Student,
   Lecturer, Rev. Father or Guest) and any other details, then **Register person**.
4. They can now scan at the door.

If the backend or WiFi is down, you can still enroll manually over the Serial Monitor:
type `enroll <slot>` (using the slot number from Admin -> People -> "Enter slot manually"),
follow the two-scan prompts, then register the person on the page with that same slot.

## How attendance works

* First scan of the day = **check in**. A later scan (at least 2 minutes after) = **check out**.
  A scan within 2 minutes of the last one is ignored as a double-scan.
* There is no late/absent/leave tracking - just who's checked in, who's checked out, and the
  running totals for the day (Admin -> Dashboard, or the live display).

## Troubleshooting

| Symptom | Fix |
|---|---|
| `UnknownHostException db....supabase.co` | Use the Session pooler host (Step 1) |
| `password authentication failed` | Wrong user format (`postgres.<ref>`) or password - reset it |
| `Tenant or user not found` | Wrong pooler region host - copy it exactly from the Connect dialog |
| Login says "Cannot reach the server" | Backend not running, or wrong port |
| ESP32 shows "Device key rejected" | `DEVICE_API_KEY` mismatch - see backend console or Admin -> Devices |
| ESP32 shows "Server error"/HTTP -1 | Wrong `BACKEND_BASE_URL`, firewall, or different WiFi network |
| "Not enrolled" on the kiosk | That sensor slot has no one registered - register a person with the same slot |
| "Scan finger" button stays on "Place a finger..." | Kiosk hasn't polled yet (~3s) or is offline - check Admin -> Devices |
