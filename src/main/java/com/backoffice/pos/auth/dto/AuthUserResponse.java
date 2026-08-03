package com.backoffice.pos.auth.dto;

import com.backoffice.pos.auth.PrincipalType;

public record AuthUserResponse(
        Long id,
        PrincipalType type,
        String name,
        String email,
        Long businessId
) {
}
