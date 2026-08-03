package com.backoffice.pos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordResetDtos {

    private PasswordResetDtos() {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record VerifyOtpRequest(@NotBlank @Email String email, @NotBlank String code) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank String code,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
    ) {
    }
}
