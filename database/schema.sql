-- Users Table
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  student_id VARCHAR(50) UNIQUE NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
  department VARCHAR(100),
  fingerprint_id INT,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT NOW()
);

-- Devices Table
CREATE TABLE devices (
  id SERIAL PRIMARY KEY,
  device_name VARCHAR(100) NOT NULL,
  device_code VARCHAR(50) UNIQUE NOT NULL,
  location VARCHAR(100),
  status VARCHAR(20) DEFAULT 'ONLINE',
  last_seen TIMESTAMP DEFAULT NOW()
);

-- Fingerprints Table
CREATE TABLE fingerprints (
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id),
  fingerprint_slot INT NOT NULL,
  device_id INT REFERENCES devices(id),
  registered_date TIMESTAMP DEFAULT NOW(),
  status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- Attendance Table
CREATE TABLE attendance (
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id),
  device_id INT REFERENCES devices(id),
  check_in TIMESTAMP,
  check_out TIMESTAMP,
  date DATE DEFAULT CURRENT_DATE,
  status VARCHAR(20) DEFAULT 'PRESENT',
  created_at TIMESTAMP DEFAULT NOW()
);