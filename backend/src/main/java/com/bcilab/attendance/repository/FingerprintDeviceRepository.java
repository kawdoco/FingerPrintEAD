package com.bcilab.attendance.repository;

import com.bcilab.attendance.model.FingerprintDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FingerprintDeviceRepository extends JpaRepository<FingerprintDevice, Long> {
    Optional<FingerprintDevice> findByApiKey(String apiKey);
    Optional<FingerprintDevice> findByDeviceCode(String deviceCode);
}
