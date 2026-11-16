package com.backoffice.pos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Minimal signup: just the owner's name, email and password. All other details
 * (business name, address, store vertical, …) are collected after the first
 * login — the owner must create their first store (onboarding) before using the
 * app, and the business profile is editable in Settings.
 */
public record SignupRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
) {
}
