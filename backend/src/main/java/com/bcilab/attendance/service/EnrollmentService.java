package com.bcilab.attendance.service;

import com.bcilab.attendance.dto.EnrollmentSessionDto;

import java.util.Optional;

public interface EnrollmentService {

    /** Admin clicked "Scan finger": arm a kiosk to capture the next finger into a free slot. */
    EnrollmentSessionDto start(String requestedDeviceCode);

    EnrollmentSessionDto getStatus(Long id);

    /** Polled by the kiosk itself: is there a capture command waiting for it right now? */
    Optional<EnrollmentSessionDto> pendingForDevice(String deviceCode);

    /** Kiosk reports it has (or hasn't) captured the two-touch scan. */
    void reportResult(String deviceCode, Long sessionId, boolean success, String message);
}
