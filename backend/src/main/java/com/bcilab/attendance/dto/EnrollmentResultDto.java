package com.bcilab.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Posted by the ESP32 once it has attempted (or given up on) a two-touch fingerprint capture. */
@Getter
@Setter
public class EnrollmentResultDto {
    @NotNull
    private Long sessionId;

    @NotNull
    private Boolean success;

    private String message;
}
