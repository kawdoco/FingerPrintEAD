package com.bcilab.attendance.service.impl;

import com.bcilab.attendance.dto.DeviceDto;
import com.bcilab.attendance.dto.DeviceRegistrationDto;
import com.bcilab.attendance.exception.DuplicateResourceException;
import com.bcilab.attendance.mapper.DtoMapper;
import com.bcilab.attendance.model.FingerprintDevice;
import com.bcilab.attendance.model.enums.DeviceStatus;
import com.bcilab.attendance.repository.FingerprintDeviceRepository;
import com.bcilab.attendance.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final FingerprintDeviceRepository deviceRepository;

    @Override
    @Transactional
    public DeviceDto register(DeviceRegistrationDto dto) {
        String code = dto.getDeviceCode().trim();
        if (deviceRepository.findByDeviceCode(code).isPresent()) {
            throw new DuplicateResourceException("Device code already registered: " + code);
        }

        FingerprintDevice device = FingerprintDevice.builder()
                .deviceCode(code)
                .location(dto.getLocation().trim())
                .apiKey(generateApiKey())
                .status(DeviceStatus.OFFLINE)
                .build();

        // The key is included ONLY in this response - copy it into the ESP32 firmware now.
        return DtoMapper.toDto(deviceRepository.save(device), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceDto> getAll() {
        return deviceRepository.findAll().stream().map(d -> DtoMapper.toDto(d, false)).toList();
    }

    @Override
    @Transactional
    public void markSeen(String deviceCode) {
        deviceRepository.findByDeviceCode(deviceCode).ifPresent(device -> {
            device.setLastSeenAt(OffsetDateTime.now());
            device.setStatus(DeviceStatus.ONLINE);
            deviceRepository.save(device);
        });
    }

    public static String generateApiKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
