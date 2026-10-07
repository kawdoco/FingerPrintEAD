package com.bcilab.attendance.dto;

import java.time.OffsetDateTime;

/** Flat, JSON-safe view of an attendance record (used by REST and the WebSocket feed). */
public record AttendanceRecordDto(
        Long id,
        Long personId,
        String idNumber,
        String personName,
        String personType,
        String department,
        String checkType,
        OffsetDateTime scannedAt,
        String deviceCode) {
}
