package com.bci.attendance_backend.service;

import com.bci.attendance_backend.entity.Fingerprint;
import com.bci.attendance_backend.repository.FingerprintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class FingerprintService {
    @Autowired
    private FingerprintRepository fingerprintRepository;

    public Fingerprint registerFingerprint(Fingerprint fingerprint) {
        fingerprint.setRegisteredDate(LocalDate.now());
        fingerprint.setStatus("ACTIVE");
        return fingerprintRepository.save(fingerprint);
    }

    public Fingerprint getByUserId(Long userId) {
        return fingerprintRepository.findByUserId(userId);
    }

    public void deleteFingerprint(Long id) {
        fingerprintRepository.deleteById(id);
    }
}