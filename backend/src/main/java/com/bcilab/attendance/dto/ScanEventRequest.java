package com.bcilab.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Payload the ESP32 posts to /api/v1/scan every time the R307S matches a finger. */
@Getter
@Setter
public class ScanEventRequest {

    /** Slot number the R307S reported for the matched finger. */
    @NotNull
    private Integer fingerprintTemplateId;

    /** Optional and informational only - the device is identified by its API key. */
    private String deviceCode;

    /** "AUTO" (default: first scan = check in, later scan = check out), "CHECK_IN" or "CHECK_OUT". */
    private String checkType;
}
