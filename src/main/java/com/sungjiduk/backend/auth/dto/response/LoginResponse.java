package com.sungjiduk.backend.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
