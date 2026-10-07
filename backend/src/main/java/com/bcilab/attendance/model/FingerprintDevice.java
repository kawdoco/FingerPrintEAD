package com.bcilab.attendance.model;

import com.bcilab.attendance.model.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fingerprint_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FingerprintDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Friendly code, e.g. "LAB204-R307S-01" (silk-screened on the enclosure) */
    @Column(name = "device_code", nullable = false, unique = true, length = 60)
    private String deviceCode;

    @Column(nullable = false, length = 120)
    private String location;

    /** Shared secret sent by the ESP32 in the X-Device-Key header. Store hashed in prod. */
    @Column(name = "api_key", nullable = false, unique = true, length = 100)
    private String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;
}
