package com.bci.attendance_backend.repository;

import com.bci.attendance_backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Device findByDeviceCode(String deviceCode);
}