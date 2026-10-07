-- ============================================================================
-- Row Level Security (defense-in-depth on top of Spring Security auth)
-- ============================================================================
-- The Spring Boot backend is the only client that talks to Supabase directly
-- (using the Supabase service-role / a dedicated "app" Postgres role), and it
-- already enforces JWT / device-API-key authorization in SecurityConfig.java.
-- These policies exist so that if anyone ever points the Supabase anon/public
-- key straight at the database (e.g. from a future mobile app), they still
-- cannot read or write attendance data without going through the API.
--
-- Run after schema.sql.

alter table departments           enable row level security;
alter table people                enable row level security;
alter table fingerprint_devices   enable row level security;
alter table attendance_records    enable row level security;
alter table enrollment_sessions   enable row level security;
alter table admin_users           enable row level security;

-- Block all access from the anon/public Supabase role by default.
-- (No policy = no rows visible/writable for roles other than the table owner.)
--
-- For the setup described in ARCHITECTURE.md (Spring Boot as the sole
-- data-access layer), no additional policies are required — the app's
-- Postgres role should bypass RLS (`bypassrls`) or use the service_role key.
