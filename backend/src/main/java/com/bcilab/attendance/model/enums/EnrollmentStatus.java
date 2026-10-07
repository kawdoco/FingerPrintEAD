package com.bcilab.attendance.model.enums;

/**
 * Lifecycle of a web-triggered fingerprint enrollment:
 * admin clicks "Scan finger" (PENDING) -> kiosk picks it up (WAITING_FINGER)
 * -> the two-touch scan succeeds (CAPTURED) or fails (FAILED) -> or nobody
 * scanned in time (EXPIRED).
 */
public enum EnrollmentStatus {
    PENDING,
    WAITING_FINGER,
    CAPTURED,
    FAILED,
    EXPIRED
}
