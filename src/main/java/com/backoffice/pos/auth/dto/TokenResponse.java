package com.backoffice.pos.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AuthUserResponse user
) {
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn, AuthUserResponse user) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
