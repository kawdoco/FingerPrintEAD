package com.bcilab.attendance.dto;

import java.time.OffsetDateTime;

public record EnrollmentSessionDto(
        Long id,
        String deviceCode,
        Integer templateId,
        String status,
        String message,
        OffsetDateTime createdAt) {
}
