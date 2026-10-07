package com.bcilab.attendance.controller;

import com.bcilab.attendance.dto.ApiResponse;
import com.bcilab.attendance.dto.DeviceDto;
import com.bcilab.attendance.dto.DeviceRegistrationDto;
import com.bcilab.attendance.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin only - provision a new kiosk and get its API key (shown once). */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ApiResponse<DeviceDto> register(@Valid @RequestBody DeviceRegistrationDto dto) {
        return ApiResponse.ok(
                "Device registered - copy the apiKey into the ESP32 firmware now, it will not be shown again",
                deviceService.register(dto));
    }

    @GetMapping
    public ApiResponse<List<DeviceDto>> getAll() {
        return ApiResponse.ok(deviceService.getAll());
    }
}
