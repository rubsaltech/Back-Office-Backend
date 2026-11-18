package com.backoffice.pos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Confirms the signup email-verification code and creates the account. */
public record SignupVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank String code
) {
}
