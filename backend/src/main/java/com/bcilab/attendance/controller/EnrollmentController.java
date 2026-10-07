package com.bcilab.attendance.controller;

import com.bcilab.attendance.dto.ApiResponse;
import com.bcilab.attendance.dto.EnrollmentSessionDto;
import com.bcilab.attendance.dto.StartEnrollmentDto;
import com.bcilab.attendance.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-facing half of the "Scan finger" button on the Register person page.
 * The kiosk-facing half (polling for the command, reporting the result)
 * lives on ScanController since kiosks authenticate with a device API key.
 */
@RestController
@RequestMapping("/api/v1/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/start")
    public ApiResponse<EnrollmentSessionDto> start(@RequestBody(required = false) StartEnrollmentDto dto) {
        String deviceCode = dto != null ? dto.deviceCode() : null;
        return ApiResponse.ok(enrollmentService.start(deviceCode));
    }

    @GetMapping("/{id}")
    public ApiResponse<EnrollmentSessionDto> status(@PathVariable Long id) {
        return ApiResponse.ok(enrollmentService.getStatus(id));
    }
}
