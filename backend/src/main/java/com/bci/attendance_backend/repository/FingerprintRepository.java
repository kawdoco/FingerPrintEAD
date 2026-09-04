package com.bci.attendance_backend.repository;

import com.bci.attendance_backend.entity.Fingerprint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FingerprintRepository extends JpaRepository<Fingerprint, Long> {
    Fingerprint findByUserId(Long userId);
}