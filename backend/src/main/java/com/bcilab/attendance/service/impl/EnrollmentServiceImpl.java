package com.bcilab.attendance.service.impl;

import com.bcilab.attendance.dto.EnrollmentSessionDto;
import com.bcilab.attendance.exception.BusinessRuleException;
import com.bcilab.attendance.exception.ResourceNotFoundException;
import com.bcilab.attendance.mapper.DtoMapper;
import com.bcilab.attendance.model.EnrollmentSession;
import com.bcilab.attendance.model.FingerprintDevice;
import com.bcilab.attendance.model.enums.EnrollmentStatus;
import com.bcilab.attendance.repository.EnrollmentSessionRepository;
import com.bcilab.attendance.repository.FingerprintDeviceRepository;
import com.bcilab.attendance.service.EnrollmentService;
import com.bcilab.attendance.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentSessionRepository enrollmentSessionRepository;
    private final FingerprintDeviceRepository deviceRepository;
    private final PersonService personService;

    @Override
    @Transactional
    public EnrollmentSessionDto start(String requestedDeviceCode) {
        FingerprintDevice device = resolveDevice(requestedDeviceCode);

        if (!enrollmentSessionRepository.findActiveForDevice(device.getDeviceCode()).isEmpty()) {
            throw new BusinessRuleException(
                    "A scan is already in progress on " + device.getDeviceCode() + ". Wait for it to finish.");
        }

        int slot = personService.nextFreeTemplateId();

        EnrollmentSession session = EnrollmentSession.builder()
                .device(device)
                .templateId(slot)
                .status(EnrollmentStatus.PENDING)
                .message("Waiting for the kiosk to pick up the command")
                .build();

        return DtoMapper.toDto(enrollmentSessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentSessionDto getStatus(Long id) {
        return enrollmentSessionRepository.findById(id)
                .map(DtoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment session not found: " + id));
    }

    @Override
    @Transactional
    public Optional<EnrollmentSessionDto> pendingForDevice(String deviceCode) {
        List<EnrollmentSession> active = enrollmentSessionRepository.findActiveForDevice(deviceCode);
        if (active.isEmpty()) {
            return Optional.empty();
        }
        EnrollmentSession session = active.get(0);
        if (session.getStatus() == EnrollmentStatus.PENDING) {
            session.setStatus(EnrollmentStatus.WAITING_FINGER);
            session.setUpdatedAt(OffsetDateTime.now());
            enrollmentSessionRepository.save(session);
        }
        return Optional.of(DtoMapper.toDto(session));
    }

    @Override
    @Transactional
    public void reportResult(String deviceCode, Long sessionId, boolean success, String message) {
        EnrollmentSession session = enrollmentSessionRepository.findByIdAndDevice_DeviceCode(sessionId, deviceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment session not found for this device"));

        session.setStatus(success ? EnrollmentStatus.CAPTURED : EnrollmentStatus.FAILED);
        session.setMessage(message);
        session.setUpdatedAt(OffsetDateTime.now());
        enrollmentSessionRepository.save(session);
    }

    private FingerprintDevice resolveDevice(String requestedDeviceCode) {
        if (requestedDeviceCode != null && !requestedDeviceCode.isBlank()) {
            return deviceRepository.findByDeviceCode(requestedDeviceCode.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + requestedDeviceCode));
        }
        List<FingerprintDevice> all = deviceRepository.findAll();
        if (all.isEmpty()) {
            throw new BusinessRuleException("No kiosk is registered yet. Add one under Devices first.");
        }
        return all.stream()
                .filter(d -> d.getStatus() == com.bcilab.attendance.model.enums.DeviceStatus.ONLINE)
                .findFirst()
                .orElse(all.get(0));
    }
}
