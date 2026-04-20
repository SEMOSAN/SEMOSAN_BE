package com.semosan.api.domain.auth.dto.response;

public record LoginResponse(
        Long userId,
        String accessToken,
        String refreshToken
) {}
