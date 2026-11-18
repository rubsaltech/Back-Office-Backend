package com.backoffice.pos.auth;

import com.backoffice.pos.auth.dto.PasswordResetDtos.ForgotPasswordRequest;
import com.backoffice.pos.auth.dto.PasswordResetDtos.ResetPasswordRequest;
import com.backoffice.pos.auth.dto.PasswordResetDtos.ValidateTokenRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    /** Email a password-reset link. */
    @PostMapping("/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        service.request(request.email());
    }

    /** Check the reset link is still valid (called when the reset page loads). */
    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validate(@Valid @RequestBody ValidateTokenRequest request) {
        service.validate(request.token());
    }

    /** Set a new password using the reset-link token. */
    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        service.reset(request.token(), request.newPassword());
    }
}
