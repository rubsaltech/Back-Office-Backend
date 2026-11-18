package com.backoffice.pos.auth;

import com.backoffice.pos.business.Business;
import com.backoffice.pos.business.BusinessRepository;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.mail.EmailService;
import com.backoffice.pos.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int LINK_TTL_MINUTES = 30;

    private final BusinessRepository businesses;
    private final PasswordResetOtpRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final String webBaseUrl;

    public PasswordResetService(BusinessRepository businesses, PasswordResetOtpRepository tokens,
                                PasswordEncoder passwordEncoder, EmailService emailService, JwtService jwtService,
                                @Value("${app.web.base-url}") String webBaseUrl) {
        this.businesses = businesses;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.webBaseUrl = webBaseUrl;
    }

    /** Emails a reset link. Always succeeds (never reveals whether the email exists). */
    @Transactional
    public void request(String email) {
        businesses.findByEmailIgnoreCase(email).ifPresent(b -> {
            String token = jwtService.generateLinkToken();
            PasswordResetOtp row = new PasswordResetOtp();
            row.setEmail(email);
            row.setTokenHash(jwtService.hashToken(token));
            row.setExpiresAt(Instant.now().plus(LINK_TTL_MINUTES, ChronoUnit.MINUTES));
            tokens.save(row);

            String link = webBaseUrl + "/business/reset-password?token=" + token;
            if (emailService.isEnabled()) {
                emailService.sendPasswordResetLink(email, link, LINK_TTL_MINUTES);
            } else {
                log.info("Password reset link for {}: {} (valid {} min) — SMTP not configured", email, link, LINK_TTL_MINUTES);
            }
        });
    }

    /** Validate a reset token without consuming it (for the reset page to load). */
    @Transactional(readOnly = true)
    public void validate(String token) {
        resolve(token);
    }

    @Transactional
    public void reset(String token, String newPassword) {
        PasswordResetOtp row = resolve(token);
        Business business = businesses.findByEmailIgnoreCase(row.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid reset request"));
        business.setPasswordHash(passwordEncoder.encode(newPassword));
        businesses.save(business);
        row.setUsed(true);
        tokens.save(row);
    }

    private PasswordResetOtp resolve(String token) {
        PasswordResetOtp row = tokens.findByTokenHashAndUsedFalse(jwtService.hashToken(token))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This reset link is invalid or has already been used"));
        if (row.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This reset link has expired");
        }
        return row;
    }
}
