# Database Schema

Postgres, hosted on Supabase. Source of truth: `database/schema.sql`
(matches the JPA entities in `backend/.../model/` exactly).

## Entity-relationship overview

```
departments 1───* people 1───* attendance_records *───1 fingerprint_devices
                                        │
                        fingerprint_devices 1───* enrollment_sessions

admin_users  (standalone — dashboard login only)
```

## Tables

### `departments`
| Column | Type | Notes |
|---|---|---|
| id | bigserial PK | |
| name | varchar(120) unique | e.g. "Neuro Systems" |

### `people`
Students, lecturers, Rev. Fathers and guests — anyone who can scan in.

| Column | Type | Notes |
|---|---|---|
| id | bigserial PK | |
| person_type | varchar(20) | `STUDENT` / `LECTURER` / `REV_FATHER` / `GUEST` |
| full_name | varchar(150) | required |
| id_number | varchar(40) nullable | student/staff number — usually blank for guests |
| email | varchar(150) nullable | |
| phone | varchar(20) nullable | |
| department_id | FK → departments, nullable | usually null for guests and visiting Rev. Fathers |
| note | varchar(150) nullable | free text, e.g. a guest's organisation |
| fingerprint_template_id | integer unique | **the R307S sensor's slot number for this person — see privacy note below** |
| active | boolean | inactive people can't check in (`BusinessRuleException`) |
| enrolled_at | timestamptz | |

### `fingerprint_devices`
| Column | Type | Notes |
|---|---|---|
| id | bigserial PK | |
| device_code | varchar(60) unique | e.g. "LAB204-R307S-01" |
| location | varchar(120) | e.g. "Lab Room 204 — Main Door" |
| api_key | varchar(100) unique | shared secret the ESP32 sends in `X-Device-Key` |
| status | ONLINE / OFFLINE | flipped by `DeviceHealthScheduler` if silent > 2 min |
| last_seen_at | timestamptz nullable | |

### `attendance_records`
| Column | Type | Notes |
|---|---|---|
| id | bigserial PK | |
| person_id | FK → people, `on delete cascade` | |
| device_id | FK → fingerprint_devices, `on delete set null` | |
| check_type | CHECK_IN / CHECK_OUT | no late/present/absent status — just the two check types |
| scanned_at | timestamptz | indexed for the live feed / date-range queries |

### `enrollment_sessions`
Backs the "Scan finger" button on the People page.

| Column | Type | Notes |
|---|---|---|
| id | bigserial PK | |
| device_id | FK → fingerprint_devices | which kiosk is listening |
| template_id | integer | the slot reserved for this capture |
| status | PENDING / WAITING_FINGER / CAPTURED / FAILED / EXPIRED | |
| message | varchar(200) nullable | e.g. "Captured" or a failure reason |
| created_at / updated_at | timestamptz | expired by `DeviceHealthScheduler` after 60s idle |

### `admin_users`
| Column | Type | Notes |
|---|---|---|
| id | bigserial PK | |
| username | varchar(80) unique | |
| password_hash | varchar(255) | BCrypt — matches Spring Security's `BCryptPasswordEncoder` |
| role | SUPER_ADMIN / LAB_COORDINATOR | |

## Biometric data — what is and isn't stored

Only an **integer template slot number** (`fingerprint_template_id`,
1–127) is stored in Postgres. The R307S sensor itself holds the actual
fingerprint templates in its own onboard flash and does all image capture
and matching locally — no fingerprint image or biometric template ever
leaves the sensor or crosses the network. If a device is ever
decommissioned, its stored templates should be wiped on the sensor
(`finger.emptyDatabase()` in the Adafruit library) independently of
anything in this database.

## Indexes

- `idx_attendance_person_time (person_id, scanned_at)` — per-person
  history queries (`/attendance/person/{id}`).
- `idx_attendance_scanned_at (scanned_at)` — today's-feed and date-range
  queries.
- `idx_people_department`, `idx_people_type` — filtering by department or
  person type.

## Row Level Security

See `database/rls_policies.sql`. RLS is enabled on every table but no
public policies are defined by default — the Spring Boot backend connects
with a role that bypasses RLS (Supabase service role, or a dedicated
Postgres role with `bypassrls`), since all authorization already happens
in `SecurityConfig.java`. This is defense-in-depth in case a future client
(e.g. a mobile app) ever queries Supabase directly.
