package com.bcilab.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRegistrationDto {
    @NotBlank
    private String deviceCode;

    @NotBlank
    private String location;
}
