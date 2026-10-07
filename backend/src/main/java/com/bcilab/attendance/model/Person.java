package com.bcilab.attendance.model;

import com.bcilab.attendance.model.enums.PersonType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Anyone enrolled to scan into the lab: a student, a lecturer, a Rev. Father
 * or a guest. Not every field applies to every type (a guest usually has no
 * ID number or department), so only fullName, personType and the fingerprint
 * slot are required.
 */
@Entity
@Table(name = "people", uniqueConstraints = {
        @UniqueConstraint(columnNames = "fingerprint_template_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 20)
    private PersonType personType;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** Student/staff ID number, or blank for guests. */
    @Column(name = "id_number", length = 40)
    private String idNumber;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    /** Department/section, or null for guests and visiting Rev. Fathers. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Free-text note - e.g. a guest's organisation, or a visiting lecturer's institution. */
    @Column(length = 150)
    private String note;

    /**
     * The slot/template ID the R307S sensor was told to store this
     * person's fingerprint under (sensor library's finger_id).
     * This is what the ESP32 sends back after a successful match.
     */
    @Column(name = "fingerprint_template_id", nullable = false)
    private Integer fingerprintTemplateId;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "enrolled_at", nullable = false)
    private OffsetDateTime enrolledAt;

    @PrePersist
    void onCreate() {
        if (enrolledAt == null) {
            enrolledAt = OffsetDateTime.now();
        }
    }
}
