package com.bcilab.attendance.repository;

import com.bcilab.attendance.model.EnrollmentSession;
import com.bcilab.attendance.model.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EnrollmentSessionRepository extends JpaRepository<EnrollmentSession, Long> {

    @Query("select e from EnrollmentSession e where e.device.deviceCode = :deviceCode "
            + "and e.status in (com.bcilab.attendance.model.enums.EnrollmentStatus.PENDING, "
            + "com.bcilab.attendance.model.enums.EnrollmentStatus.WAITING_FINGER) "
            + "order by e.createdAt asc")
    List<EnrollmentSession> findActiveForDevice(@Param("deviceCode") String deviceCode);

    @Query("select e from EnrollmentSession e where e.status in "
            + "(com.bcilab.attendance.model.enums.EnrollmentStatus.PENDING, "
            + "com.bcilab.attendance.model.enums.EnrollmentStatus.WAITING_FINGER) "
            + "and e.createdAt < :cutoff")
    List<EnrollmentSession> findStaleActive(@Param("cutoff") OffsetDateTime cutoff);

    Optional<EnrollmentSession> findByIdAndDevice_DeviceCode(Long id, String deviceCode);
}
