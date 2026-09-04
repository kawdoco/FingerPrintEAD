package com.bci.attendance_backend.service;

import com.bci.attendance_backend.entity.Attendance;
import com.bci.attendance_backend.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceService {
    @Autowired
    private AttendanceRepository attendanceRepository;

    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    public List<Attendance> getAttendanceByUser(Long userId) {
        return attendanceRepository.findByUserId(userId);
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    public Attendance recordCheckIn(Long userId, String deviceId) {
        Attendance attendance = new Attendance();
        attendance.setUserId(userId);
        attendance.setDeviceId(deviceId);
        attendance.setCheckIn(LocalDateTime.now());
        attendance.setDate(LocalDate.now());
        attendance.setStatus(determineStatus(LocalDateTime.now()));
        return attendanceRepository.save(attendance);
    }

    public Attendance recordCheckOut(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId).orElseThrow();
        attendance.setCheckOut(LocalDateTime.now());
        return attendanceRepository.save(attendance);
    }

    private String determineStatus(LocalDateTime checkInTime) {
        int hour = checkInTime.getHour();
        int minute = checkInTime.getMinute();
        if (hour > 8 || (hour == 8 && minute > 15)) {
            return "LATE";
        }
        return "PRESENT";
    }
}