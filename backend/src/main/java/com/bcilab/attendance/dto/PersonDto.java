package com.bcilab.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonDto {
    private Long id;

    /** STUDENT, LECTURER, REV_FATHER or GUEST. */
    @NotBlank
    private String personType;

    @NotBlank
    private String fullName;

    /** Optional: student/staff/ID number. Blank for most guests. */
    private String idNumber;

    private String email;

    private String phone;

    /** Optional: department/section name. Left blank creates no department link. */
    private String departmentName;

    /** Optional free-text note (e.g. guest's organisation). */
    private String note;

    @NotNull
    private Integer fingerprintTemplateId;

    private boolean active;
}
