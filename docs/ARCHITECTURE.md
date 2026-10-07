# Architecture

## 1. Overview

```
┌────────────┐     UART      ┌───────────┐     WiFi / HTTPS      ┌──────────────────┐     JDBC      ┌───────────┐
│  R307S     │◄─────────────►│   ESP32   │──────POST /scan──────►│  Spring Boot API │──────────────►│  Supabase │
│  sensor    │                │  + TFT +  │                        │  (MVC, JWT +     │                │ (Postgres)│
│            │                │  buzzer   │◄─────scan result──────│   device auth)   │◄──────────────│           │
└────────────┘                └───────────┘                        └─────────┬────────┘                └───────────┘
                                                                                │ REST + WebSocket
                                                                                ▼
                                                                    ┌──────────────────────┐
                                                                    │   React frontend     │
                                                                    │  • Live display (TV) │
                                                                    │  • Admin dashboard   │
                                                                    │  • Register person   │
                                                                    └──────────────────────┘
```

Three physical deployment points on campus:

1. **The lab door** — an ESP32 kiosk with the R307S sensor and a 3.5" TFT.
2. **A TV/monitor in the lab** — runs the React "Live Display" page
   (`/`), showing recent check-ins/check-outs and today's counts.
3. **Anywhere** — the lab coordinator uses the same React app's `/admin`
   routes from a laptop or phone to register people and review attendance.

Anyone who scans in — a **student**, **lecturer**, **Rev. Father** or
**guest** — is a single `Person` record distinguished by `personType`. Only
their name, type and fingerprint slot are required; ID number, email,
phone and department are all optional, since most guests and visiting
clergy won't have a department or staff number.

## 2. Backend — MVC layering

The backend follows a classic layered MVC/Spring pattern; each layer only
talks to the one below it:

```
controller/   HTTP in, DTOs out. No business logic. Validates request bodies
              (@Valid), delegates to a service, wraps results in ApiResponse<T>.

service/      Business rules live here: e.g. check-in vs check-out resolution
              and double-scan handling (AttendanceServiceImpl), the "scan
              finger" enrollment workflow (EnrollmentServiceImpl), device
              API-key issuance. Interfaces in service/, implementations in
              service/impl/.

repository/   Spring Data JPA interfaces — no SQL written by hand except in
              a couple of @Query methods for date-range filtering.

model/        JPA entities, one per table in database/schema.sql. DTOs are
              used wherever the shape needs to differ from the entity
              (PersonDto, ScanResultResponse, EnrollmentSessionDto, etc.).
```

Supporting packages:

- `dto/` — request/response shapes that aren't 1:1 with an entity.
- `exception/` + `GlobalExceptionHandler` — turns thrown exceptions into a
  consistent `{ success, message, data }` JSON envelope with the right HTTP
  status.
- `security/` — `JwtService` (issue/verify tokens), `JwtAuthenticationFilter`
  (admin dashboard auth), `DeviceApiKeyFilter` (kiosk auth via
  `X-Device-Key` header) — see §4.
- `mapper/DtoMapper` — converts entities to flat DTOs inside the transaction, so lazy
  associations never leak into JSON.
- `config/DataInitializer` — creates the first admin, first kiosk and optional demo
  people (a mix of person types) on an empty database.
- `ws/` — `AttendanceFeedPublisher`, a WebSocket handler that pushes every
  new attendance record to connected dashboards instantly, so the lab TV
  doesn't need to poll.
- `scheduler/` — `DeviceHealthScheduler` flips a kiosk to `OFFLINE` if it
  hasn't posted anything in 2 minutes, and expires an enrollment session if
  nobody finishes the "scan finger" flow within 60 seconds.

## 3. Attendance data flow (the core use case)

1. A person places a finger on the R307S. The sensor matches it locally
   against its stored templates and returns a **template slot number**
   (an integer, 1–127) — not an image, not raw biometric data.
2. The ESP32 sends that number to `POST /api/v1/scan`, authenticated with a
   per-device API key.
3. `ScanController` → `AttendanceServiceImpl.recordScan()`:
   - Looks up the `Person` by `fingerprintTemplateId`.
   - Resolves the scan: first scan of the day = `CHECK_IN`, later = `CHECK_OUT`, and a repeat
     within 2 minutes is ignored as a double-scan.
   - Saves an `AttendanceRecord`. There is no late/present/absent status — just
     check-in and check-out.
   - Publishes it over the WebSocket for any connected dashboards.
   - Returns the person's name + check type so the kiosk's TFT and buzzer can
     give immediate feedback.
4. The React `LiveDisplay` page receives the pushed record and updates the
   feed instantly; if the socket drops, it falls back to polling
   `GET /api/v1/attendance/live` and `/today/summary` every 5 seconds
   (`frontend/src/hooks/useAttendanceFeed.js`).

## 4. Registering a new person ("Scan finger" workflow)

Instead of typing `enroll <slot>` into the Arduino Serial Monitor, an admin
on the **People** page clicks **Scan finger**:

1. Frontend calls `POST /api/v1/enrollment/start`, which creates an
   `EnrollmentSession` for the chosen (or only) kiosk and reserves the next
   free sensor slot.
2. The kiosk polls `GET /api/v1/scan/enrollment-pending` every few seconds.
   When it sees the session, its status flips to `WAITING_FINGER` and the
   kiosk prompts on the TFT.
3. The kiosk runs the same two-touch capture used by the manual `enroll`
   command, then reports success/failure to
   `POST /api/v1/scan/enrollment-result`.
4. The frontend, which has been polling `GET /api/v1/enrollment/{id}`, sees
   `status: "CAPTURED"` and reveals the registration form (name, person
   type, department, etc.) pre-filled with the captured slot.
5. Submitting the form calls `POST /api/v1/people/from-enrollment/{sessionId}`,
   which creates the `Person` using that slot.

The manual Serial Monitor path (`enroll <slot>` + `POST /api/v1/people`)
still works as a fallback when the backend or WiFi is unreachable.

## 5. Authentication model

Two independent schemes coexist in one `SecurityFilterChain`
(`SecurityConfig.java`):

| Caller                        | Scheme                              | Endpoints                                   |
|--------------------------------|--------------------------------------|----------------------------------------------|
| ESP32 kiosk                    | Shared secret in `X-Device-Key` header, checked by `DeviceApiKeyFilter` | `POST /api/v1/scan`, `/scan/heartbeat`, `/scan/enrollment-pending`, `/scan/enrollment-result` |
| Lab TV                         | None (public reads by design) | `GET /attendance/live`, `/attendance/today/summary` |
| Admin / lab coordinator (React dashboard) | JWT bearer token from `/auth/login`, checked by `JwtAuthenticationFilter` | Everything else under `/api/v1/**`, including People and Enrollment |

Device keys are generated server-side (`DeviceServiceImpl.generateApiKey`,
32 random bytes, base64url) when an admin registers a kiosk, and shown
**once** in the registration response — copy it into the firmware
immediately.

## 6. Why Supabase here

Supabase is used purely as **managed Postgres** — the Spring Boot backend
connects over JDBC like it would to any Postgres instance
(`spring.datasource.url` in `application.yml`), and is the *only* service
that talks to the database directly. `database/rls_policies.sql` enables
Row Level Security as a defense-in-depth measure in case a client key is
ever pointed at Supabase directly in the future, but today all
authorization is enforced in Spring Security, not in Postgres policies.

## 7. Extensibility notes

- **Multiple lab doors**: just register another device
  (`POST /api/v1/devices`) and flash a second ESP32 with its own device
  code/key — no backend changes needed. The People page's device selector
  picks it up automatically.
- **Another person type**: add a value to `PersonType` (backend) and to the
  `PERSON_TYPES` list in `frontend/src/theme/classes.js` — the rest of the
  UI (badges, form, history) already reads from that list.
- **Mobile app later**: since all business logic lives in the service
  layer behind REST, a future mobile client is just another consumer of
  the same API — no duplication needed.
