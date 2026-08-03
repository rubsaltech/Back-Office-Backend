package com.backoffice.pos.staff.dto;

import com.backoffice.pos.staff.Permission;

import java.time.Instant;

public record PermissionResponse(
        Long id,
        String key,
        String description,
        Instant createdAt
) {
    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getKey(), p.getDescription(), p.getCreatedAt());
    }
}
