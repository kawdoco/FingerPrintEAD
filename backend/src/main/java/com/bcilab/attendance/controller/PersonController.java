package com.bcilab.attendance.controller;

import com.bcilab.attendance.dto.ApiResponse;
import com.bcilab.attendance.dto.PersonDto;
import com.bcilab.attendance.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin only. Two ways to enrol someone:
 *  1. Recommended: POST /api/v1/enrollment/start, wait for CAPTURED, then
 *     POST here with the enrollmentSessionId (see EnrollmentController).
 *  2. Manual: ask /next-template-id for a free slot, type "enroll <slot>" in
 *     the kiosk's Serial Monitor, then create the person here with that slot.
 */
@RestController
@RequestMapping("/api/v1/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping
    public ApiResponse<PersonDto> create(@Valid @RequestBody PersonDto dto) {
        return ApiResponse.ok("Registered", personService.create(dto));
    }

    @PostMapping("/from-enrollment/{sessionId}")
    public ApiResponse<PersonDto> createFromEnrollment(@PathVariable Long sessionId, @Valid @RequestBody PersonDto dto) {
        return ApiResponse.ok("Registered", personService.createFromEnrollment(sessionId, dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<PersonDto> update(@PathVariable Long id, @Valid @RequestBody PersonDto dto) {
        return ApiResponse.ok(personService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        personService.delete(id);
        return ApiResponse.ok("Removed", null);
    }

    @GetMapping("/next-template-id")
    public ApiResponse<Map<String, Integer>> nextTemplateId() {
        return ApiResponse.ok(Map.of("templateId", personService.nextFreeTemplateId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PersonDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(personService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<PersonDto>> getAll() {
        return ApiResponse.ok(personService.getAll());
    }
}
