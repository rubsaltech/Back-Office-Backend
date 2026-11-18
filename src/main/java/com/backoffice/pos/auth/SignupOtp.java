package com.backoffice.pos.auth;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A pending signup awaiting email verification. The account is created only once
 * the emailed code is confirmed; until then the owner's name and (already hashed)
 * password are parked here. Only hashes of the code and password are stored.
 */
@Entity
@Table(name = "signup_otps")
@Getter
@Setter
public class SignupOtp extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}
