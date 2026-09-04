package com.bci.attendance_backend.service;

import com.bci.attendance_backend.entity.Device;
import com.bci.attendance_backend.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Device registerDevice(Device device) {
        device.setStatus("ONLINE");
        device.setLastSeen(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    public void updateLastSeen(String deviceCode) {
        Device device = deviceRepository.findByDeviceCode(deviceCode);
        if (device != null) {
            device.setLastSeen(LocalDateTime.now());
            device.setStatus("ONLINE");
            deviceRepository.save(device);
        }
    }
}