package com.bci.attendance_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "attendance")
@Data
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String deviceId;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private LocalDate date;
    private String status;
}