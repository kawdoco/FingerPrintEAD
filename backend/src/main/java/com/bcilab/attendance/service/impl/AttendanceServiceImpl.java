package com.bcilab.attendance.service.impl;

import com.bcilab.attendance.config.AppProperties;
import com.bcilab.attendance.dto.AttendanceRecordDto;
import com.bcilab.attendance.dto.AttendanceSummaryDto;
import com.bcilab.attendance.dto.ScanEventRequest;
import com.bcilab.attendance.dto.ScanResultResponse;
import com.bcilab.attendance.exception.BusinessRuleException;
import com.bcilab.attendance.exception.ResourceNotFoundException;
import com.bcilab.attendance.mapper.DtoMapper;
import com.bcilab.attendance.model.AttendanceRecord;
import com.bcilab.attendance.model.FingerprintDevice;
import com.bcilab.attendance.model.Person;
import com.bcilab.attendance.model.enums.CheckType;
import com.bcilab.attendance.repository.AttendanceRepository;
import com.bcilab.attendance.repository.FingerprintDeviceRepository;
import com.bcilab.attendance.repository.PersonRepository;
import com.bcilab.attendance.service.AttendanceService;
import com.bcilab.attendance.ws.AttendanceFeedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    /** A second scan within this window is treated as an accidental double-scan and ignored. */
    private static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(2);

    private final AttendanceRepository attendanceRepository;
    private final PersonRepository personRepository;
    private final FingerprintDeviceRepository deviceRepository;
    private final AppProperties appProperties;
    private final AttendanceFeedPublisher feedPublisher;

    private ZoneId zone() {
        return ZoneId.of(appProperties.getAttendance().getTimezone());
    }

    @Override
    @Transactional
    public ScanResultResponse recordScan(ScanEventRequest request, String deviceCode) {
        Person person = personRepository.findByFingerprintTemplateId(request.getFingerprintTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No one enrolled with fingerprint slot " + request.getFingerprintTemplateId()));

        if (!person.isActive()) {
            throw new BusinessRuleException("This record is inactive: " + person.getFullName());
        }

        FingerprintDevice device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + deviceCode));

        ZoneId zone = zone();
        OffsetDateTime now = OffsetDateTime.now(zone);
        LocalDate today = now.toLocalDate();
        OffsetDateTime dayStart = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dayEnd = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        // newest first
        List<AttendanceRecord> todays = attendanceRepository.findByPersonBetween(person.getId(), dayStart, dayEnd);
        AttendanceRecord last = todays.isEmpty() ? null : todays.get(0);

        String raw = request.getCheckType() == null ? "AUTO" : request.getCheckType().trim().toUpperCase();
        boolean auto = raw.isBlank() || raw.equals("AUTO");
        CheckType requested = auto ? null : parseExplicit(raw);

        // Accidental double scan -> return the earlier record, write nothing.
        if (last != null && Duration.between(last.getScannedAt(), now).compareTo(DUPLICATE_WINDOW) < 0
                && (auto || last.getCheckType() == requested)) {
            return ScanResultResponse.builder()
                    .personName(person.getFullName())
                    .personType(person.getPersonType().name())
                    .departmentName(person.getDepartment() != null ? person.getDepartment().getName() : null)
                    .checkType(last.getCheckType())
                    .duplicate(true)
                    .scannedAt(last.getScannedAt())
                    .displayMessage(person.getFullName() + " - already recorded")
                    .build();
        }

        CheckType checkType;
        if (auto) {
            checkType = (last != null && last.getCheckType() == CheckType.CHECK_IN)
                    ? CheckType.CHECK_OUT : CheckType.CHECK_IN;
        } else {
            checkType = requested;
        }

        AttendanceRecord record = attendanceRepository.save(AttendanceRecord.builder()
                .person(person)
                .device(device)
                .checkType(checkType)
                .scannedAt(now)
                .build());

        feedPublisher.publish(DtoMapper.toDto(record));

        return ScanResultResponse.builder()
                .personName(person.getFullName())
                .personType(person.getPersonType().name())
                .departmentName(person.getDepartment() != null ? person.getDepartment().getName() : null)
                .checkType(checkType)
                .duplicate(false)
                .scannedAt(record.getScannedAt())
                .displayMessage(buildDisplayMessage(person.getFullName(), checkType))
                .build();
    }

    private CheckType parseExplicit(String raw) {
        try {
            return CheckType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Unknown checkType: " + raw + " (use AUTO, CHECK_IN or CHECK_OUT)");
        }
    }

    private String buildDisplayMessage(String name, CheckType checkType) {
        return checkType == CheckType.CHECK_OUT ? name + " - checked out" : name + " - checked in";
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getLiveFeed(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        ZoneId zone = zone();
        OffsetDateTime dayStart = LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime();
        return attendanceRepository.findLatestSince(dayStart, PageRequest.of(0, safeLimit))
                .stream().map(DtoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryDto getTodaySummary() {
        ZoneId zone = zone();
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime from = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        List<AttendanceRecord> records = attendanceRepository.findAllBetween(from, to);

        // Latest scan per person today decides whether they are still "in".
        Map<Long, AttendanceRecord> latestByPerson = new HashMap<>();
        for (AttendanceRecord r : records) {
            Long personId = r.getPerson().getId();
            AttendanceRecord existing = latestByPerson.get(personId);
            if (existing == null || r.getScannedAt().isAfter(existing.getScannedAt())) {
                latestByPerson.put(personId, r);
            }
        }
        long inLab = latestByPerson.values().stream()
                .filter(r -> r.getCheckType() == CheckType.CHECK_IN)
                .count();

        long totalPeople = personRepository.countByActiveTrue();

        return new AttendanceSummaryDto(inLab, records.size(), totalPeople);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getRecordsForDate(LocalDate date) {
        ZoneId zone = zone();
        OffsetDateTime from = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return attendanceRepository.findAllBetween(from, to).stream().map(DtoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getRecordsForPerson(Long personId, LocalDate from, LocalDate to) {
        ZoneId zone = zone();
        OffsetDateTime fromDt = from.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime toDt = to.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return attendanceRepository.findByPersonBetween(personId, fromDt, toDt)
                .stream().map(DtoMapper::toDto).toList();
    }
}
