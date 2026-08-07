package com.backoffice.pos.auth;

import com.backoffice.pos.business.Business;
import com.backoffice.pos.business.BusinessRepository;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.mail.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_TTL_MINUTES = 10;

    private final BusinessRepository businesses;
    private final PasswordResetOtpRepository otps;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(BusinessRepository businesses, PasswordResetOtpRepository otps,
                                PasswordEncoder passwordEncoder, EmailService emailService) {
        this.businesses = businesses;
        this.otps = otps;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** Always succeeds (does not reveal whether the email exists). */
    @Transactional
    public void request(String email) {
        businesses.findByEmailIgnoreCase(email).ifPresent(b -> {
            String code = String.format("%04d", RANDOM.nextInt(10_000));
            PasswordResetOtp otp = new PasswordResetOtp();
            otp.setEmail(email);
            otp.setCodeHash(passwordEncoder.encode(code));
            otp.setExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
            otps.save(otp);

            if (emailService.isEnabled()) {
                // Fire-and-forget on a background thread (handles its own errors).
                emailService.sendPasswordResetOtp(email, code, OTP_TTL_MINUTES);
            } else {
                // Dev fallback (no SMTP configured): log the code so it can be used.
                log.info("Password reset OTP for {} is {} (valid {} min) — SMTP not configured", email, code, OTP_TTL_MINUTES);
            }
        });
    }

    @Transactional(readOnly = true)
    public void verify(String email, String code) {
        validate(email, code);
    }

    @Transactional
    public void reset(String email, String code, String newPassword) {
        PasswordResetOtp otp = validate(email, code);
        Business business = businesses.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid reset request"));
        business.setPasswordHash(passwordEncoder.encode(newPassword));
        businesses.save(business);
        otp.setUsed(true);
        otps.save(otp);
    }

    private PasswordResetOtp validate(String email, String code) {
        PasswordResetOtp otp = otps.findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired code"));
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Code has expired");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired code");
        }
        return otp;
    }
}
