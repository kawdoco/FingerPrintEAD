package com.bcilab.attendance.dto;

import java.time.OffsetDateTime;

/** apiKey is only filled in on the registration response, never in list responses. */
public record DeviceDto(
        Long id,
        String deviceCode,
        String location,
        String status,
        OffsetDateTime lastSeenAt,
        String apiKey) {
}
