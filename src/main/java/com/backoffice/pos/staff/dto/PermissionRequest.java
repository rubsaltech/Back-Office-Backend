package com.backoffice.pos.staff.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequest(
        @NotBlank String key,
        String description
) {
}
