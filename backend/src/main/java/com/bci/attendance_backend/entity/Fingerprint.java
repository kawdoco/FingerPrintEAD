package com.bci.attendance_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "fingerprints")
@Data
public class Fingerprint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Integer fingerprintSlot;
    private String deviceId;
    private LocalDate registeredDate;
    private String status;
}