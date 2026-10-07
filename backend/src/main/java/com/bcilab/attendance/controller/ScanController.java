package com.bcilab.attendance.controller;

import com.bcilab.attendance.dto.ApiResponse;
import com.bcilab.attendance.dto.EnrollmentResultDto;
import com.bcilab.attendance.dto.EnrollmentSessionDto;
import com.bcilab.attendance.dto.HeartbeatRequest;
import com.bcilab.attendance.dto.ScanEventRequest;
import com.bcilab.attendance.dto.ScanResultResponse;
import com.bcilab.attendance.service.AttendanceService;
import com.bcilab.attendance.service.DeviceService;
import com.bcilab.attendance.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called only by the ESP32 kiosk. Auth: X-Device-Key header (DeviceApiKeyFilter).
 * The device identity comes from the key itself, not from the request body.
 */
@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
public class ScanController {

    private final AttendanceService attendanceService;
    private final DeviceService deviceService;
    private final EnrollmentService enrollmentService;

    @PostMapping
    public ApiResponse<ScanResultResponse> scan(@Valid @RequestBody ScanEventRequest request,
                                                Authentication authentication) {
        String deviceCode = authentication.getName();
        deviceService.markSeen(deviceCode);
        return ApiResponse.ok(attendanceService.recordScan(request, deviceCode));
    }

    /** Lightweight "I'm alive" ping so the dashboard shows the kiosk as ONLINE even when nobody is scanning. */
    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@RequestBody(required = false) HeartbeatRequest ignored,
                                       Authentication authentication) {
        deviceService.markSeen(authentication.getName());
        return ApiResponse.ok(null);
    }

    /** Polled every loop: is the admin dashboard waiting for this kiosk to capture a new finger? */
    @GetMapping("/enrollment-pending")
    public ApiResponse<EnrollmentSessionDto> enrollmentPending(Authentication authentication) {
        return ApiResponse.ok(enrollmentService.pendingForDevice(authentication.getName()).orElse(null));
    }

    /** Reported once the kiosk's two-touch capture has succeeded or failed. */
    @PostMapping("/enrollment-result")
    public ApiResponse<Void> enrollmentResult(@Valid @RequestBody EnrollmentResultDto dto,
                                              Authentication authentication) {
        enrollmentService.reportResult(authentication.getName(), dto.getSessionId(),
                Boolean.TRUE.equals(dto.getSuccess()), dto.getMessage());
        return ApiResponse.ok(null);
    }
}
