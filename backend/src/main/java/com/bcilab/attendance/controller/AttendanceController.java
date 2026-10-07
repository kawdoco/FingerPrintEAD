package com.bcilab.attendance.controller;

import com.bcilab.attendance.dto.ApiResponse;
import com.bcilab.attendance.dto.AttendanceRecordDto;
import com.bcilab.attendance.dto.AttendanceSummaryDto;
import com.bcilab.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** Public: today's latest check-ins/check-outs for the lab screen (polling fallback for the WebSocket). */
    @GetMapping("/live")
    public ApiResponse<List<AttendanceRecordDto>> live(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(attendanceService.getLiveFeed(limit));
    }

    /** Public: in-lab / total-scans / total-people counts for today. */
    @GetMapping("/today/summary")
    public ApiResponse<AttendanceSummaryDto> todaySummary() {
        return ApiResponse.ok(attendanceService.getTodaySummary());
    }

    /** Admin: all records for a date (defaults to today). */
    @GetMapping
    public ApiResponse<List<AttendanceRecordDto>> forDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(attendanceService.getRecordsForDate(date != null ? date : LocalDate.now()));
    }

    /** Admin: one person's history over a date range. */
    @GetMapping("/person/{personId}")
    public ApiResponse<List<AttendanceRecordDto>> forPerson(
            @PathVariable Long personId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceService.getRecordsForPerson(personId, from, to));
    }
}
