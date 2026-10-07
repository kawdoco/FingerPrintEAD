package com.bcilab.attendance.service.impl;

import com.bcilab.attendance.dto.PersonDto;
import com.bcilab.attendance.exception.BusinessRuleException;
import com.bcilab.attendance.exception.DuplicateResourceException;
import com.bcilab.attendance.exception.ResourceNotFoundException;
import com.bcilab.attendance.mapper.DtoMapper;
import com.bcilab.attendance.model.Department;
import com.bcilab.attendance.model.EnrollmentSession;
import com.bcilab.attendance.model.Person;
import com.bcilab.attendance.model.enums.EnrollmentStatus;
import com.bcilab.attendance.model.enums.PersonType;
import com.bcilab.attendance.repository.DepartmentRepository;
import com.bcilab.attendance.repository.EnrollmentSessionRepository;
import com.bcilab.attendance.repository.PersonRepository;
import com.bcilab.attendance.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    /** Adafruit_Fingerprint safely addresses slots 1..127. */
    private static final int MAX_TEMPLATE_ID = 127;

    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;
    private final EnrollmentSessionRepository enrollmentSessionRepository;

    @Override
    @Transactional
    public PersonDto create(PersonDto dto) {
        validateUnique(dto, null);
        Person person = fromDto(new Person(), dto);
        return DtoMapper.toDto(personRepository.save(person));
    }

    @Override
    @Transactional
    public PersonDto update(Long id, PersonDto dto) {
        Person person = personRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + id));
        validateUnique(dto, id);
        fromDto(person, dto);
        return DtoMapper.toDto(personRepository.save(person));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!personRepository.existsById(id)) {
            throw new ResourceNotFoundException("Person not found: " + id);
        }
        personRepository.deleteById(id);
        personRepository.flush(); // surface FK violations here so the handler can answer 409
    }

    @Override
    @Transactional(readOnly = true)
    public PersonDto getById(Long id) {
        return personRepository.findByIdWithDepartment(id)
                .map(DtoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonDto> getAll() {
        return personRepository.findAllWithDepartment().stream().map(DtoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int nextFreeTemplateId() {
        Set<Integer> used = new HashSet<>(personRepository.findAllTemplateIds());
        for (int id = 1; id <= MAX_TEMPLATE_ID; id++) {
            if (!used.contains(id)) {
                return id;
            }
        }
        throw new DuplicateResourceException("All fingerprint slots 1-" + MAX_TEMPLATE_ID + " are in use");
    }

    @Override
    @Transactional
    public PersonDto createFromEnrollment(Long enrollmentSessionId, PersonDto dto) {
        EnrollmentSession session = enrollmentSessionRepository.findById(enrollmentSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment session not found: " + enrollmentSessionId));

        if (session.getStatus() != EnrollmentStatus.CAPTURED) {
            throw new BusinessRuleException(
                    "That fingerprint scan is not ready yet (status: " + session.getStatus() + ")");
        }
        if (personRepository.existsByFingerprintTemplateId(session.getTemplateId())) {
            throw new DuplicateResourceException(
                    "Fingerprint slot " + session.getTemplateId() + " is already assigned to another person");
        }

        dto.setFingerprintTemplateId(session.getTemplateId());
        validateUnique(dto, null);
        Person person = fromDto(new Person(), dto);
        return DtoMapper.toDto(personRepository.save(person));
    }

    private void validateUnique(PersonDto dto, Long excludeId) {
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            boolean clash = excludeId == null
                    ? personRepository.existsByEmailIgnoreCase(dto.getEmail().trim())
                    : personRepository.existsByEmailIgnoreCaseAndIdNot(dto.getEmail().trim(), excludeId);
            if (clash) {
                throw new DuplicateResourceException("Someone is already registered with email " + dto.getEmail());
            }
        }
        boolean slotTaken = excludeId == null
                ? personRepository.existsByFingerprintTemplateId(dto.getFingerprintTemplateId())
                : personRepository.existsByFingerprintTemplateIdAndIdNot(dto.getFingerprintTemplateId(), excludeId);
        if (slotTaken) {
            throw new DuplicateResourceException(
                    "Fingerprint slot " + dto.getFingerprintTemplateId() + " is already assigned to another person");
        }
    }

    private Person fromDto(Person person, PersonDto dto) {
        person.setPersonType(parseType(dto.getPersonType()));
        person.setFullName(dto.getFullName().trim());
        person.setIdNumber(blankToNull(dto.getIdNumber()));
        person.setEmail(blankToNull(dto.getEmail()));
        person.setPhone(blankToNull(dto.getPhone()));
        person.setNote(blankToNull(dto.getNote()));
        person.setDepartment(resolveDepartment(dto.getDepartmentName()));
        person.setFingerprintTemplateId(dto.getFingerprintTemplateId());
        person.setActive(person.getId() == null || dto.isActive());
        return person;
    }

    private PersonType parseType(String raw) {
        try {
            return PersonType.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessRuleException(
                    "Unknown person type: " + raw + " (use STUDENT, LECTURER, REV_FATHER or GUEST)");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private Department resolveDepartment(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.trim();
        return departmentRepository.findByNameIgnoreCase(clean)
                .orElseGet(() -> departmentRepository.save(Department.builder().name(clean).build()));
    }
}
