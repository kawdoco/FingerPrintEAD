package com.bci.attendance_backend.controller;

import com.bci.attendance_backend.entity.Attendance;
import com.bci.attendance_backend.service.AttendanceService;
import com.bci.attendance_backend.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private DeviceService deviceService;

    @GetMapping
    public List<Attendance> getAllAttendance() {
        return attendanceService.getAllAttendance();
    }

    @GetMapping("/{id}")
    public List<Attendance> getByUser(@PathVariable("id") Long userId) {
        return attendanceService.getAttendanceByUser(userId);
    }

    // Matches the ESP32 payload: { "deviceId": "...", "fingerprintId": 12, "timestamp": "..." }
    @PostMapping
    public Attendance recordAttendance(@RequestBody Map<String, Object> payload) {
        String deviceId = (String) payload.get("deviceId");
        Long userId = Long.valueOf(payload.get("fingerprintId").toString());
        deviceService.updateLastSeen(deviceId);
        return attendanceService.recordCheckIn(userId, deviceId);
    }
}