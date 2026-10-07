package com.bcilab.attendance.service;

import com.bcilab.attendance.dto.AttendanceRecordDto;
import com.bcilab.attendance.dto.AttendanceSummaryDto;
import com.bcilab.attendance.dto.ScanEventRequest;
import com.bcilab.attendance.dto.ScanResultResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    /** Called from the device-authenticated /api/v1/scan endpoint. deviceCode comes from the API key. */
    ScanResultResponse recordScan(ScanEventRequest request, String deviceCode);

    /** Today's most recent scans (for the lab screen). */
    List<AttendanceRecordDto> getLiveFeed(int limit);

    AttendanceSummaryDto getTodaySummary();

    List<AttendanceRecordDto> getRecordsForDate(LocalDate date);

    List<AttendanceRecordDto> getRecordsForPerson(Long personId, LocalDate from, LocalDate to);
}
