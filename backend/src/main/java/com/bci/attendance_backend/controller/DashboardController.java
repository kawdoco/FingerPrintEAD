package com.bci.attendance_backend.controller;

import com.bci.attendance_backend.service.AttendanceService;
import com.bci.attendance_backend.service.UserService;
import com.bci.attendance_backend.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired
    private UserService userService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private DeviceService deviceService;

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.getAllUsers().size());
        stats.put("presentToday", attendanceService.getAttendanceByDate(LocalDate.now()).size());
        stats.put("totalDevices", deviceService.getAllDevices().size());
        return stats;
    }

    @GetMapping("/attendance")
    public Object getAttendanceOverview() {
        return attendanceService.getAttendanceByDate(LocalDate.now());
    }

    @GetMapping("/devices")
    public Object getDeviceOverview() {
        return deviceService.getAllDevices();
    }
}