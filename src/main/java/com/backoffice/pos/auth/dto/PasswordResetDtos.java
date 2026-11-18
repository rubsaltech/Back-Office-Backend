package com.backoffice.pos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordResetDtos {

    private PasswordResetDtos() {
    }

    /** Request a reset link to be emailed. */
    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    /** Check a reset-link token is still valid (used when the reset page loads). */
    public record ValidateTokenRequest(@NotBlank String token) {
    }

    /** Set a new password using the reset-link token. */
    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
    ) {
    }
}
