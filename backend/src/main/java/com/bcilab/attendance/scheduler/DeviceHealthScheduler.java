package com.bcilab.attendance.scheduler;

import com.bcilab.attendance.model.EnrollmentSession;
import com.bcilab.attendance.model.FingerprintDevice;
import com.bcilab.attendance.model.enums.DeviceStatus;
import com.bcilab.attendance.model.enums.EnrollmentStatus;
import com.bcilab.attendance.repository.EnrollmentSessionRepository;
import com.bcilab.attendance.repository.FingerprintDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * A device is marked OFFLINE if it hasn't posted a scan (or heartbeat) in
 * 2 minutes, so the admin dashboard's device status list stays honest even
 * if a reader loses power or WiFi in Lab Room 204. Also expires "scan to
 * register" sessions nobody finished within 60 seconds.
 */
@Component
@RequiredArgsConstructor
public class DeviceHealthScheduler {

    private final FingerprintDeviceRepository deviceRepository;
    private final EnrollmentSessionRepository enrollmentSessionRepository;
    private static final int OFFLINE_THRESHOLD_MINUTES = 2;
    private static final int ENROLLMENT_TIMEOUT_SECONDS = 60;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void flagStaleDevices() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(OFFLINE_THRESHOLD_MINUTES);
        for (FingerprintDevice device : deviceRepository.findAll()) {
            boolean stale = device.getLastSeenAt() == null || device.getLastSeenAt().isBefore(cutoff);
            if (stale && device.getStatus() == DeviceStatus.ONLINE) {
                device.setStatus(DeviceStatus.OFFLINE);
                deviceRepository.save(device);
            }
        }
    }

    @Scheduled(fixedRate = 15_000)
    @Transactional
    public void expireStaleEnrollmentSessions() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusSeconds(ENROLLMENT_TIMEOUT_SECONDS);
        for (EnrollmentSession session : enrollmentSessionRepository.findStaleActive(cutoff)) {
            session.setStatus(EnrollmentStatus.EXPIRED);
            session.setMessage("No finger scanned in time");
            enrollmentSessionRepository.save(session);
        }
    }
}
