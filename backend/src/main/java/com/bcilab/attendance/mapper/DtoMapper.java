package com.bcilab.attendance.mapper;

import com.bcilab.attendance.dto.AttendanceRecordDto;
import com.bcilab.attendance.dto.DeviceDto;
import com.bcilab.attendance.dto.EnrollmentSessionDto;
import com.bcilab.attendance.dto.PersonDto;
import com.bcilab.attendance.model.AttendanceRecord;
import com.bcilab.attendance.model.EnrollmentSession;
import com.bcilab.attendance.model.FingerprintDevice;
import com.bcilab.attendance.model.Person;

/**
 * Entity -> DTO conversion. Call these INSIDE a transaction (or on fetch-joined
 * entities) because person/department are lazy associations.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static AttendanceRecordDto toDto(AttendanceRecord r) {
        Person p = r.getPerson();
        return new AttendanceRecordDto(
                r.getId(),
                p.getId(),
                p.getIdNumber(),
                p.getFullName(),
                p.getPersonType().name(),
                p.getDepartment() != null ? p.getDepartment().getName() : null,
                r.getCheckType().name(),
                r.getScannedAt(),
                r.getDevice() != null ? r.getDevice().getDeviceCode() : null);
    }

    public static DeviceDto toDto(FingerprintDevice d, boolean includeKey) {
        return new DeviceDto(
                d.getId(),
                d.getDeviceCode(),
                d.getLocation(),
                d.getStatus().name(),
                d.getLastSeenAt(),
                includeKey ? d.getApiKey() : null);
    }

    public static PersonDto toDto(Person p) {
        PersonDto dto = new PersonDto();
        dto.setId(p.getId());
        dto.setPersonType(p.getPersonType().name());
        dto.setFullName(p.getFullName());
        dto.setIdNumber(p.getIdNumber());
        dto.setEmail(p.getEmail());
        dto.setPhone(p.getPhone());
        dto.setDepartmentName(p.getDepartment() != null ? p.getDepartment().getName() : null);
        dto.setNote(p.getNote());
        dto.setFingerprintTemplateId(p.getFingerprintTemplateId());
        dto.setActive(p.isActive());
        return dto;
    }

    public static EnrollmentSessionDto toDto(EnrollmentSession s) {
        return new EnrollmentSessionDto(
                s.getId(),
                s.getDevice().getDeviceCode(),
                s.getTemplateId(),
                s.getStatus().name(),
                s.getMessage(),
                s.getCreatedAt());
    }
}
