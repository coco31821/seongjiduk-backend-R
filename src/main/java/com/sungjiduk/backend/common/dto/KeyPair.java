package com.sungjiduk.backend.common.dto;

public record KeyPair(
        String accessToken,
        String refreshToken
) {
}
