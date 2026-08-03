package com.backoffice.pos.staff.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record RoleRequest(
        @NotBlank String name,
        String description,
        Set<Long> permissionIds
) {
}
