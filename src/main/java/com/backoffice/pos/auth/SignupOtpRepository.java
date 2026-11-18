package com.backoffice.pos.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignupOtpRepository extends JpaRepository<SignupOtp, Long> {

    Optional<SignupOtp> findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(String email);
}
