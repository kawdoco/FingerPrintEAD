package com.bcilab.attendance.model;

import com.bcilab.attendance.model.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * A one-off "please scan a new finger into slot N" command handed to a kiosk
 * from the admin dashboard's Register person page (instead of typing
 * "enroll N" into the Arduino Serial Monitor).
 */
@Entity
@Table(name = "enrollment_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private FingerprintDevice device;

    @Column(name = "template_id", nullable = false)
    private Integer templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Column(length = 200)
    private String message;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
