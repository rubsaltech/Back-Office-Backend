package com.backoffice.pos.auth;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A one-time password for the forgot-password flow (only the hash is stored). */
@Entity
@Table(name = "password_reset_otps")
@Getter
@Setter
public class PasswordResetOtp extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}
