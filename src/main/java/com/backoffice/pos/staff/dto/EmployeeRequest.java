package com.backoffice.pos.staff.dto;

import com.backoffice.pos.staff.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmployeeRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String password,   // set on create; optional on update (blank = keep)
        String pin,        // terminal PIN; optional on update (blank = keep)
        Long storeId,
        Long roleId,
        EmployeeStatus status
) {
}
