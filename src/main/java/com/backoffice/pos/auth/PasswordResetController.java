package com.backoffice.pos.auth;

import com.backoffice.pos.auth.dto.PasswordResetDtos.ForgotPasswordRequest;
import com.backoffice.pos.auth.dto.PasswordResetDtos.ResetPasswordRequest;
import com.backoffice.pos.auth.dto.PasswordResetDtos.VerifyOtpRequest;
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

    @PostMapping("/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        service.request(request.email());
    }

    @PostMapping("/verify-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody VerifyOtpRequest request) {
        service.verify(request.email(), request.code());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        service.reset(request.email(), request.code(), request.newPassword());
    }
}
