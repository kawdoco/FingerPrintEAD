package com.bcilab.attendance.service;

import com.bcilab.attendance.dto.DeviceDto;
import com.bcilab.attendance.dto.DeviceRegistrationDto;

import java.util.List;

public interface DeviceService {
    DeviceDto register(DeviceRegistrationDto dto);
    List<DeviceDto> getAll();
    void markSeen(String deviceCode);
}
