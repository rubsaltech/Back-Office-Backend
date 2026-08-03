package com.backoffice.pos.security;

import com.backoffice.pos.auth.PrincipalType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Issues and validates JWT access tokens and opaque refresh tokens. */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_BUSINESS = "bid";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_AUTHORITIES = "auth";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

    public String generateAccessToken(AuthPrincipal principal, Collection<String> authorities) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(principal.id()))
                .claim(CLAIM_TYPE, principal.type().name())
                .claim(CLAIM_BUSINESS, principal.businessId())
                .claim(CLAIM_NAME, principal.displayName())
                .claim(CLAIM_AUTHORITIES, List.copyOf(authorities))
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(props.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long businessId = claims.get(CLAIM_BUSINESS, Number.class) == null
                ? null
                : claims.get(CLAIM_BUSINESS, Number.class).longValue();

        AuthPrincipal principal = new AuthPrincipal(
                Long.valueOf(claims.getSubject()),
                PrincipalType.valueOf(claims.get(CLAIM_TYPE, String.class)),
                businessId,
                claims.get(CLAIM_NAME, String.class)
        );

        @SuppressWarnings("unchecked")
        List<String> auths = claims.get(CLAIM_AUTHORITIES, List.class);
        List<GrantedAuthority> authorities = (auths == null ? List.<String>of() : auths).stream()
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a))
                .toList();

        return new ParsedToken(principal, authorities);
    }

    public Instant refreshTokenExpiry() {
        return Instant.now().plus(props.refreshTokenTtl());
    }

    /** A fresh opaque refresh-token value (returned to the client). */
    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    /** SHA-256 (Base64) of a refresh token — only the hash is stored. */
    public String hashRefreshToken(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash refresh token", e);
        }
    }

    public record ParsedToken(AuthPrincipal principal, List<GrantedAuthority> authorities) {
    }
}
