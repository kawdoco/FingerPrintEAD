-- ============================================================================
-- BCI Research Lab Attendance System — Supabase (Postgres) schema
-- Run this in Supabase Studio: Project -> SQL Editor -> New query -> Run.
-- Matches the JPA entities in backend/src/main/java/.../model/*.java exactly,
-- so keep the two in sync if you change one.
-- ============================================================================

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- departments
-- ---------------------------------------------------------------------------
create table if not exists departments (
    id          bigserial primary key,
    name        varchar(120) not null unique
);

-- ---------------------------------------------------------------------------
-- people  (students, lecturers, Rev. Fathers, guests - anyone who can scan in)
-- ---------------------------------------------------------------------------
create table if not exists people (
    id                          bigserial primary key,
    person_type                 varchar(20)  not null check (person_type in ('STUDENT', 'LECTURER', 'REV_FATHER', 'GUEST')),
    full_name                   varchar(150) not null,
    id_number                   varchar(40),
    email                       varchar(150),
    phone                       varchar(20),
    department_id               bigint       references departments(id),
    note                        varchar(150),
    fingerprint_template_id     integer      not null unique,
    active                      boolean      not null default true,
    enrolled_at                 timestamptz  not null default now()
);

create index if not exists idx_people_department on people(department_id);
create index if not exists idx_people_type on people(person_type);

-- ---------------------------------------------------------------------------
-- fingerprint_devices  (one row per ESP32 + R307S kiosk, see esp32/ firmware)
-- ---------------------------------------------------------------------------
create table if not exists fingerprint_devices (
    id              bigserial primary key,
    device_code     varchar(60)  not null unique,
    location        varchar(120) not null,
    api_key         varchar(100) not null unique,
    status          varchar(20)  not null default 'OFFLINE' check (status in ('ONLINE', 'OFFLINE')),
    last_seen_at    timestamptz
);

-- ---------------------------------------------------------------------------
-- attendance_records  (just check-in / check-out - no late/absent/leave status)
-- ---------------------------------------------------------------------------
create table if not exists attendance_records (
    id              bigserial primary key,
    person_id       bigint      not null references people(id) on delete cascade,
    device_id       bigint      references fingerprint_devices(id) on delete set null,
    check_type      varchar(20) not null check (check_type in ('CHECK_IN', 'CHECK_OUT')),
    scanned_at      timestamptz not null default now()
);

create index if not exists idx_attendance_person_time on attendance_records(person_id, scanned_at);
create index if not exists idx_attendance_scanned_at on attendance_records(scanned_at);

-- ---------------------------------------------------------------------------
-- enrollment_sessions  (the "Scan finger" button on the Register person page)
-- ---------------------------------------------------------------------------
create table if not exists enrollment_sessions (
    id              bigserial primary key,
    device_id       bigint      not null references fingerprint_devices(id) on delete cascade,
    template_id     integer     not null,
    status          varchar(20) not null default 'PENDING'
                        check (status in ('PENDING', 'WAITING_FINGER', 'CAPTURED', 'FAILED', 'EXPIRED')),
    message         varchar(200),
    created_at      timestamptz not null default now(),
    updated_at      timestamptz
);

-- ---------------------------------------------------------------------------
-- admin_users  (lab coordinators / super admins who log into the React dashboard)
-- ---------------------------------------------------------------------------
create table if not exists admin_users (
    id              bigserial primary key,
    username        varchar(80)  not null unique,
    password_hash   varchar(255) not null,
    role            varchar(30)  not null check (role in ('SUPER_ADMIN', 'LAB_COORDINATOR'))
);

-- ============================================================================
-- Notes
-- ============================================================================
-- 1. You do NOT need to insert an admin by hand: on first start the backend creates one
--    from APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD (BCrypt-hashed) when admin_users is empty.
--    Running this file is optional - the backend also creates missing tables itself.
--
-- 2. fingerprint_template_id is the slot number the R307S sensor stores the
--    fingerprint under (1-127 in this project), NOT a copy of the raw
--    biometric template — no biometric image or template data is ever stored
--    in this database, only that integer slot reference.
--
-- 3. Row Level Security: see rls_policies.sql. The Spring Boot backend connects
--    with the Supabase "postgres"/service role (or a dedicated app role) and
--    enforces authorization itself, so RLS here is a defense-in-depth layer,
--    not the primary access control.
