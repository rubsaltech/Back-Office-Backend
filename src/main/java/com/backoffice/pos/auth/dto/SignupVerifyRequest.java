package com.backoffice.pos.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirms the signup verification link token and creates the account. */
public record SignupVerifyRequest(
        @NotBlank String token
) {
}
