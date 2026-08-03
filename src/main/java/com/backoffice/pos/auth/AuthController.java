package com.backoffice.pos.auth;

import com.backoffice.pos.auth.dto.AuthUserResponse;
import com.backoffice.pos.auth.dto.LoginRequest;
import com.backoffice.pos.auth.dto.PinLoginRequest;
import com.backoffice.pos.auth.dto.RefreshRequest;
import com.backoffice.pos.auth.dto.SignupRequest;
import com.backoffice.pos.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @GetMapping("/auth/me")
    public AuthUserResponse me() {
        return authService.me();
    }

    @PostMapping("/terminal/pin-login")
    public TokenResponse pinLogin(@Valid @RequestBody PinLoginRequest request) {
        return authService.pinLogin(request);
    }
}
