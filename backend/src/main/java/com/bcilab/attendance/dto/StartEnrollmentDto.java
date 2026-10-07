package com.bcilab.attendance.dto;

/** Which kiosk should listen for the next finger. Leave deviceCode blank to use the only/first online device. */
public record StartEnrollmentDto(String deviceCode) {
}
