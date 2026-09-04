package com.bci.attendance_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;
    private String fullName;
    private String password;
    private String department;
    private Integer fingerprintId;
    private String status;
}