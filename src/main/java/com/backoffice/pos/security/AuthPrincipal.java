package com.backoffice.pos.security;

import com.backoffice.pos.auth.PrincipalType;

/**
 * The authenticated caller, stored as the Spring Security principal.
 * {@code businessId} is null only for platform-admin principals.
 */
public record AuthPrincipal(
        Long id,
        PrincipalType type,
        Long businessId,
        String displayName
) {
}
