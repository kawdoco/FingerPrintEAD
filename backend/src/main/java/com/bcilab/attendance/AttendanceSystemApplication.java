package com.bcilab.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the BCI Research Lab Attendance System.
 *
 * Architecture: classic Spring MVC layering.
 *   controller  -> handles HTTP, validates input, returns DTOs
 *   service     -> business rules (check-in/out, enrollment, auth)
 *   repository  -> Spring Data JPA access to Supabase (Postgres)
 *   model       -> JPA entities mapped 1:1 to database/schema.sql
 *
 * See /docs/ARCHITECTURE.md in the project root for the full picture.
 */
@SpringBootApplication
@EnableScheduling
public class AttendanceSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(AttendanceSystemApplication.class, args);
    }
}
