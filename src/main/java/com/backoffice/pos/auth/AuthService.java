package com.backoffice.pos.auth;

import com.backoffice.pos.auth.dto.AuthUserResponse;
import com.backoffice.pos.auth.dto.LoginRequest;
import com.backoffice.pos.auth.dto.PinLoginRequest;
import com.backoffice.pos.auth.dto.SignupRequest;
import com.backoffice.pos.auth.dto.TokenResponse;
import com.backoffice.pos.business.Business;
import com.backoffice.pos.business.BusinessProvisioningService;
import com.backoffice.pos.business.BusinessRepository;
import com.backoffice.pos.business.BusinessStatus;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.mail.EmailDomainValidator;
import com.backoffice.pos.mail.EmailService;
import com.backoffice.pos.security.AuthPrincipal;
import com.backoffice.pos.security.CurrentUser;
import com.backoffice.pos.security.JwtProperties;
import com.backoffice.pos.security.JwtService;
import com.backoffice.pos.staff.Employee;
import com.backoffice.pos.staff.EmployeeRepository;
import com.backoffice.pos.staff.Permission;
import com.backoffice.pos.staff.PermissionRepository;
import com.backoffice.pos.store.Store;
import com.backoffice.pos.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int SIGNUP_LINK_TTL_MINUTES = 60;

    private final BusinessRepository businesses;
    private final EmployeeRepository employees;
    private final PermissionRepository permissions;
    private final StoreRepository stores;
    private final RefreshTokenRepository refreshTokens;
    private final SignupOtpRepository signupOtps;
    private final BusinessProvisioningService provisioning;
    private final EmailService emailService;
    private final EmailDomainValidator emailDomainValidator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final String webBaseUrl;

    public AuthService(BusinessRepository businesses, EmployeeRepository employees, PermissionRepository permissions,
                       StoreRepository stores, RefreshTokenRepository refreshTokens, SignupOtpRepository signupOtps,
                       BusinessProvisioningService provisioning, EmailService emailService,
                       EmailDomainValidator emailDomainValidator, PasswordEncoder passwordEncoder,
                       JwtService jwtService, JwtProperties jwtProperties,
                       @Value("${app.web.base-url}") String webBaseUrl) {
        this.businesses = businesses;
        this.employees = employees;
        this.permissions = permissions;
        this.stores = stores;
        this.refreshTokens = refreshTokens;
        this.signupOtps = signupOtps;
        this.provisioning = provisioning;
        this.emailService = emailService;
        this.emailDomainValidator = emailDomainValidator;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.webBaseUrl = webBaseUrl;
    }

    /**
     * Step 1 of signup: does NOT create the account. Validates the email looks
     * real (syntax + its domain can receive mail), parks the pending signup
     * (name + hashed password), and emails a verification LINK. The account is
     * created only once that link is clicked (step 2).
     */
    @Transactional
    public void startSignup(SignupRequest req) {
        if (!emailDomainValidator.hasMailDomain(req.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please enter a valid email address");
        }
        if (businesses.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        String token = jwtService.generateLinkToken();
        SignupOtp otp = new SignupOtp();
        otp.setEmail(req.email());
        otp.setName(req.name());
        otp.setPasswordHash(passwordEncoder.encode(req.password()));
        otp.setTokenHash(jwtService.hashToken(token));
        otp.setExpiresAt(Instant.now().plus(SIGNUP_LINK_TTL_MINUTES, ChronoUnit.MINUTES));
        signupOtps.save(otp);

        String link = webBaseUrl + "/business/verify-email?token=" + token;
        if (emailService.isEnabled()) {
            emailService.sendSignupVerifyLink(req.email(), link, SIGNUP_LINK_TTL_MINUTES);
        } else {
            log.info("Signup verification link for {}: {} (valid {} min) — SMTP not configured",
                    req.email(), link, SIGNUP_LINK_TTL_MINUTES);
        }
    }

    /**
     * Step 2 of signup: verifies the link token, creates the account from the
     * parked pending signup, sends a welcome email, and logs the owner in.
     */
    @Transactional
    public TokenResponse verifySignup(String token) {
        SignupOtp otp = signupOtps.findByTokenHashAndUsedFalse(jwtService.hashToken(token))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This verification link is invalid or has already been used"));
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This verification link has expired");
        }
        if (businesses.existsByEmailIgnoreCase(otp.getEmail())) {
            // Already verified/created (e.g. link clicked twice).
            throw new ConflictException("This account is already verified — please log in");
        }

        Business business = new Business();
        // Business name defaults to the owner's name until they set a real one in
        // Settings; the store they create during onboarding carries its own name.
        business.setName(otp.getName());
        business.setEmail(otp.getEmail());
        business.setPasswordHash(otp.getPasswordHash()); // already hashed in step 1
        business.setOwnerName(otp.getName());
        business.setStatus(BusinessStatus.APPROVED); // dev: auto-approved (admin gating added later)
        business = businesses.save(business);

        provisioning.provisionDefaults(business);

        otp.setUsed(true);
        signupOtps.save(otp);

        if (emailService.isEnabled()) {
            emailService.sendWelcome(business.getEmail(), business.getOwnerName());
        }
        return issueForOwner(business);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        // The email identifies either a business owner or an employee.
        Business business = businesses.findByEmailIgnoreCase(req.email()).orElse(null);
        if (business != null) {
            if (!passwordEncoder.matches(req.password(), business.getPasswordHash())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
            }
            assertBusinessUsable(business);
            return issueForOwner(business);
        }

        // Employee web login (email + password).
        Employee employee = employees.findByEmailIgnoreCase(req.email()).stream()
                .filter(e -> e.getPasswordHash() != null && passwordEncoder.matches(req.password(), e.getPasswordHash()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (employee.getStatus() == com.backoffice.pos.staff.EmployeeStatus.INACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This employee account is inactive");
        }
        assertBusinessUsable(businesses.findById(employee.getBusinessId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password")));
        return issueForEmployee(employee);
    }

    private void assertBusinessUsable(Business business) {
        if (business.getStatus() == BusinessStatus.BLOCKED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This business account is blocked");
        }
        if (business.getStatus() == BusinessStatus.DEACTIVATED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This business account is deactivated");
        }
    }

    @Transactional
    public TokenResponse pinLogin(PinLoginRequest req) {
        Store store = stores.findById(req.storeId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid store or PIN"));

        Employee match = employees.findByStores_Id(store.getId()).stream()
                .filter(e -> e.getPinHash() != null && passwordEncoder.matches(req.pin(), e.getPinHash()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid store or PIN"));
        return issueForEmployee(match);
    }

    private TokenResponse issueForEmployee(Employee employee) {
        List<String> authorities = employeePermissions(employee);
        AuthPrincipal principal = new AuthPrincipal(
                employee.getId(), PrincipalType.EMPLOYEE, employee.getBusinessId(), employee.getFullName());
        return issue(principal, authorities, employee.getEmail());
    }

    private static List<String> employeePermissions(Employee employee) {
        return employee.getRole() == null ? List.of()
                : employee.getRole().getPermissions().stream().map(Permission::getKey).toList();
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        String hash = jwtService.hashRefreshToken(refreshTokenValue);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        stored.setRevoked(true); // rotate
        refreshTokens.save(stored);

        if (stored.getPrincipalType() == PrincipalType.BUSINESS_OWNER) {
            Business business = businesses.findById(stored.getPrincipalId())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
            return issueForOwner(business);
        }
        Employee employee = employees.findById(stored.getPrincipalId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        return issueForEmployee(employee);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me() {
        AuthPrincipal principal = CurrentUser.get();
        if (principal.type() == PrincipalType.BUSINESS_OWNER) {
            Business b = businesses.findById(principal.id())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
            return new AuthUserResponse(b.getId(), principal.type(), b.getOwnerName(), b.getEmail(), b.getId(),
                    ownerPermissions(b.getId()));
        }
        Employee e = employees.findById(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        return new AuthUserResponse(e.getId(), principal.type(), e.getFullName(), e.getEmail(), e.getBusinessId(),
                employeePermissions(e));
    }

    private List<String> ownerPermissions(Long businessId) {
        return permissions.findByBusinessIdOrderByKeyAsc(businessId).stream().map(Permission::getKey).toList();
    }

    // ---- helpers ----

    private TokenResponse issueForOwner(Business business) {
        List<String> authorities = ownerPermissions(business.getId());
        AuthPrincipal principal = new AuthPrincipal(
                business.getId(), PrincipalType.BUSINESS_OWNER, business.getId(),
                business.getOwnerName() != null ? business.getOwnerName() : business.getName());
        return issue(principal, authorities, business.getEmail());
    }

    private TokenResponse issue(AuthPrincipal principal, List<String> authorities, String email) {
        String accessToken = jwtService.generateAccessToken(principal, authorities);
        String refreshValue = createRefreshToken(principal);
        AuthUserResponse user = new AuthUserResponse(
                principal.id(), principal.type(), principal.displayName(), email, principal.businessId(), authorities);
        return TokenResponse.bearer(accessToken, refreshValue, jwtProperties.accessTokenTtl().toSeconds(), user);
    }

    private String createRefreshToken(AuthPrincipal principal) {
        String value = jwtService.generateRefreshTokenValue();
        RefreshToken token = new RefreshToken();
        token.setTokenHash(jwtService.hashRefreshToken(value));
        token.setPrincipalType(principal.type());
        token.setPrincipalId(principal.id());
        token.setBusinessId(principal.businessId());
        token.setExpiresAt(jwtService.refreshTokenExpiry());
        refreshTokens.save(token);
        return value;
    }
}
