package com.bci.attendance_backend.repository;

import com.bci.attendance_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}