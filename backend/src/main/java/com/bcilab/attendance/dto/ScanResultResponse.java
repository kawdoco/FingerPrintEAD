package com.bcilab.attendance.dto;

import com.bcilab.attendance.model.enums.CheckType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/** What the backend sends back to the ESP32 so the TFT and buzzer can react. */
@Getter
@Builder
@AllArgsConstructor
public class ScanResultResponse {
    private String personName;
    private String personType;
    private String departmentName;
    private CheckType checkType;
    private boolean duplicate;
    private OffsetDateTime scannedAt;
    private String displayMessage;
}
