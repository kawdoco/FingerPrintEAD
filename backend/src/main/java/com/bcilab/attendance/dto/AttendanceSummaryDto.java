package com.bcilab.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendanceSummaryDto {
    /** People who checked in today and have not checked out yet. */
    private long inLabCount;
    /** Total check-in + check-out scans recorded today. */
    private long totalScansToday;
    /** Total active people enrolled in the system. */
    private long totalPeople;
}
