package com.backoffice.pos.staff.dto;

import com.backoffice.pos.staff.Permission;
import com.backoffice.pos.staff.Role;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        List<PermissionResponse> permissions
) {
    public static RoleResponse from(Role r) {
        List<PermissionResponse> perms = r.getPermissions().stream()
                .sorted(Comparator.comparing(Permission::getKey))
                .map(PermissionResponse::from)
                .toList();
        return new RoleResponse(r.getId(), r.getName(), r.getDescription(), r.getCreatedAt(), perms);
    }
}
