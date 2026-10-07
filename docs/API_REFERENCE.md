# API Reference

Base URL: `http://<host>:8080/api/v1`

Every response is wrapped:

```json
{ "success": true, "message": null, "data": { ... } }
```

or on error:

```json
{ "success": false, "message": "Person not found: 42", "data": null }
```

## Auth

### `POST /auth/login`  — public
```json
// request
{ "username": "admin", "password": "ChangeMe123!" }

// response.data
{ "token": "eyJhbGciOi...", "username": "admin", "role": "SUPER_ADMIN", "expiresInMinutes": 480 }
```
Send the token back as `Authorization: Bearer <token>` on every admin call.

## Device scan (ESP32 only)

Header `X-Device-Key: <device api key>` is required on every call in this section. The device
identity comes from the key, not from anything in the request body.

### `POST /scan`
```json
// request  (checkType: AUTO (default) | CHECK_IN | CHECK_OUT)
{ "fingerprintTemplateId": 3, "checkType": "AUTO" }

// response.data
{
  "personName": "Achini Silva",
  "personType": "STUDENT",
  "departmentName": "Neuro Systems",
  "checkType": "CHECK_IN",
  "duplicate": false,
  "scannedAt": "2026-09-30T09:21:04+05:30",
  "displayMessage": "Achini Silva - checked in"
}
```
AUTO: first scan of the day = check in, later scan = check out. A second scan within 2 minutes
returns the earlier record with `"duplicate": true` and writes nothing.
Errors: 404 slot not linked to anyone, 422 person inactive, 401 bad key.

### `POST /scan/heartbeat`
Empty JSON body `{}`; keeps the device `ONLINE` on the dashboard.

### `GET /scan/enrollment-pending`
Polled by the kiosk every few seconds. Returns `data: null` when nothing is waiting, or an
`EnrollmentSession` (see below) when the admin dashboard has requested a capture. The first
poll that sees a `PENDING` session flips it to `WAITING_FINGER`.

### `POST /scan/enrollment-result`
```json
{ "sessionId": 7, "success": true, "message": "Captured" }
```
Reported by the kiosk once the two-touch capture has succeeded or failed.

## Attendance

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/attendance/live?limit=8` | public | today's latest check-ins/check-outs |
| GET | `/attendance/today/summary` | public | `{ inLabCount, totalScansToday, totalPeople }` |
| GET | `/attendance?date=2026-09-30` | admin | all records for a date (default today) |
| GET | `/attendance/person/{personId}?from=2026-09-01&to=2026-09-30` | admin | one person's history |

Record shape (REST and WebSocket): `{ id, personId, idNumber, personName, personType, department, checkType, scannedAt, deviceCode }`

### Live WebSocket
`ws://<host>:8080/ws/attendance` pushes one record (shape above) per new scan.

## People (admin only)

Covers students, lecturers, Rev. Fathers and guests — anyone who can scan in.

| Method | Path | Notes |
|---|---|---|
| POST | `/people` | body: `PersonDto` (see below); manual path — pair with a slot from `/next-template-id` and `enroll <slot>` on the kiosk's Serial Monitor |
| POST | `/people/from-enrollment/{sessionId}` | body: `PersonDto`; registers the person captured by a `CAPTURED` enrollment session — this is what the "Scan finger" flow uses |
| PUT | `/people/{id}` | update |
| DELETE | `/people/{id}` | remove (409 if they have attendance records — deactivate instead) |
| GET | `/people/{id}` | fetch one |
| GET | `/people` | list all |
| GET | `/people/next-template-id` | smallest free sensor slot (1-127) |

`PersonDto`:
```json
{
  "personType": "STUDENT",
  "fullName": "New Student",
  "idNumber": "BCI/2024/014",
  "email": "new.student@bci.lk",
  "phone": "+94770000000",
  "departmentName": "Robotics",
  "note": null,
  "fingerprintTemplateId": 14,
  "active": true
}
```
`personType` is one of `STUDENT`, `LECTURER`, `REV_FATHER`, `GUEST`. Only `personType`, `fullName`
and `fingerprintTemplateId` are required — `idNumber`, `email`, `phone`, `departmentName` and `note`
are all optional (a guest usually has none of them).

## Enrollment ("Scan finger" button, admin only)

| Method | Path | Notes |
|---|---|---|
| POST | `/enrollment/start` | body: `{ "deviceCode": "LAB204-R307S-01" }` (or `{}` / omit to use the only/first online kiosk); creates a session and assigns the next free slot |
| GET | `/enrollment/{id}` | poll this until `status` is `CAPTURED`, `FAILED` or `EXPIRED` |

`EnrollmentSession`:
```json
{
  "id": 7,
  "deviceCode": "LAB204-R307S-01",
  "templateId": 9,
  "status": "WAITING_FINGER",
  "message": "Waiting for the kiosk to pick up the command",
  "createdAt": "2026-09-30T09:20:00+05:30"
}
```
`status` moves `PENDING` → `WAITING_FINGER` (kiosk picked it up) → `CAPTURED` / `FAILED`, or
`EXPIRED` if nobody scanned within 60 seconds. Once `CAPTURED`, POST the person's details to
`/people/from-enrollment/{id}`.

## Devices (admin only)

| Method | Path | Notes |
|---|---|---|
| POST | `/devices` | body: `{ "deviceCode": "...", "location": "..." }`; response includes `apiKey` **once** |
| GET | `/devices` | list kiosks with online/offline status (no keys) |

## Error codes

| Status | Meaning |
|---|---|
| 400 | Validation failure (`data` holds a field→message map) |
| 401 | Missing/invalid JWT, missing/invalid device key, or bad login |
| 404 | Resource not found (unknown person, device, enrollment session) |
| 409 | Duplicate value, or person has attendance records (deactivate instead of delete) |
| 422 | Business rule (inactive person, scan already in progress on that kiosk, enrollment not captured yet) |
| 500 | Unexpected server error |
