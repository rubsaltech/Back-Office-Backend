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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final BusinessRepository businesses;
    private final EmployeeRepository employees;
    private final PermissionRepository permissions;
    private final StoreRepository stores;
    private final RefreshTokenRepository refreshTokens;
    private final BusinessProvisioningService provisioning;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(BusinessRepository businesses, EmployeeRepository employees, PermissionRepository permissions,
                       StoreRepository stores, RefreshTokenRepository refreshTokens,
                       BusinessProvisioningService provisioning, PasswordEncoder passwordEncoder,
                       JwtService jwtService, JwtProperties jwtProperties) {
        this.businesses = businesses;
        this.employees = employees;
        this.permissions = permissions;
        this.stores = stores;
        this.refreshTokens = refreshTokens;
        this.provisioning = provisioning;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public TokenResponse signup(SignupRequest req) {
        if (businesses.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("A business with this email already exists");
        }
        Business business = new Business();
        business.setName(req.businessName());
        business.setEmail(req.email());
        business.setPasswordHash(passwordEncoder.encode(req.password()));
        business.setOwnerName(req.ownerName());
        business.setOwnerPhone(req.ownerPhone());
        business.setStatus(BusinessStatus.APPROVED); // dev: auto-approved (admin gating added later)
        business = businesses.save(business);

        provisioning.provisionDefaults(business);
        return issueForOwner(business);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        Business business = businesses.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), business.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (business.getStatus() == BusinessStatus.BLOCKED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This business account is blocked");
        }
        if (business.getStatus() == BusinessStatus.DEACTIVATED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This business account is deactivated");
        }
        return issueForOwner(business);
    }

    @Transactional
    public TokenResponse pinLogin(PinLoginRequest req) {
        Store store = stores.findById(req.storeId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid store or PIN"));

        Employee match = employees.findByStore_Id(store.getId()).stream()
                .filter(e -> e.getPinHash() != null && passwordEncoder.matches(req.pin(), e.getPinHash()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid store or PIN"));

        List<String> authorities = match.getRole() == null ? List.of()
                : match.getRole().getPermissions().stream().map(Permission::getKey).toList();

        AuthPrincipal principal = new AuthPrincipal(
                match.getId(), PrincipalType.EMPLOYEE, store.getBusinessId(), match.getFullName());
        return issue(principal, authorities, match.getEmail());
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
        List<String> authorities = employee.getRole() == null ? List.of()
                : employee.getRole().getPermissions().stream().map(Permission::getKey).toList();
        AuthPrincipal principal = new AuthPrincipal(
                employee.getId(), PrincipalType.EMPLOYEE, employee.getBusinessId(), employee.getFullName());
        return issue(principal, authorities, employee.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me() {
        AuthPrincipal principal = CurrentUser.get();
        if (principal.type() == PrincipalType.BUSINESS_OWNER) {
            Business b = businesses.findById(principal.id())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
            return new AuthUserResponse(b.getId(), principal.type(), b.getOwnerName(), b.getEmail(), b.getId());
        }
        Employee e = employees.findById(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        return new AuthUserResponse(e.getId(), principal.type(), e.getFullName(), e.getEmail(), e.getBusinessId());
    }

    // ---- helpers ----

    private TokenResponse issueForOwner(Business business) {
        List<String> authorities = permissions.findByBusinessIdOrderByKeyAsc(business.getId())
                .stream().map(Permission::getKey).toList();
        AuthPrincipal principal = new AuthPrincipal(
                business.getId(), PrincipalType.BUSINESS_OWNER, business.getId(),
                business.getOwnerName() != null ? business.getOwnerName() : business.getName());
        return issue(principal, authorities, business.getEmail());
    }

    private TokenResponse issue(AuthPrincipal principal, List<String> authorities, String email) {
        String accessToken = jwtService.generateAccessToken(principal, authorities);
        String refreshValue = createRefreshToken(principal);
        AuthUserResponse user = new AuthUserResponse(
                principal.id(), principal.type(), principal.displayName(), email, principal.businessId());
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
