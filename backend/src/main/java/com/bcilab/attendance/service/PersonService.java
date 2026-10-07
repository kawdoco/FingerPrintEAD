package com.bcilab.attendance.service;

import com.bcilab.attendance.dto.PersonDto;

import java.util.List;

public interface PersonService {
    PersonDto create(PersonDto dto);
    PersonDto update(Long id, PersonDto dto);
    void delete(Long id);
    PersonDto getById(Long id);
    List<PersonDto> getAll();

    /** Smallest unused sensor slot (1..127), so admins know which slot to enroll next. */
    int nextFreeTemplateId();

    /** Register the person captured by a completed web enrollment session, using its assigned slot. */
    PersonDto createFromEnrollment(Long enrollmentSessionId, PersonDto dto);
}
