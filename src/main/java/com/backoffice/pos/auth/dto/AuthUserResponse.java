package com.backoffice.pos.auth.dto;

import com.backoffice.pos.auth.PrincipalType;

import java.util.List;

public record AuthUserResponse(
        Long id,
        PrincipalType type,
        String name,
        String email,
        Long businessId,
        /** Permission keys the caller holds — owners get all; employees get their role's. */
        List<String> permissions
) {
}
