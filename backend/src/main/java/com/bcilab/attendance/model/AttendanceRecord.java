package com.bcilab.attendance.model;

import com.bcilab.attendance.model.enums.CheckType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "attendance_records", indexes = {
        @Index(name = "idx_attendance_person_time", columnList = "person_id, scanned_at"),
        @Index(name = "idx_attendance_scanned_at", columnList = "scanned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private FingerprintDevice device;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 20)
    private CheckType checkType;

    @Column(name = "scanned_at", nullable = false)
    private OffsetDateTime scannedAt;

    @PrePersist
    void onCreate() {
        if (scannedAt == null) {
            scannedAt = OffsetDateTime.now();
        }
    }
}
