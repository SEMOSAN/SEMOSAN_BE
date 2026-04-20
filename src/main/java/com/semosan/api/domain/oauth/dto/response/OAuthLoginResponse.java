package com.semosan.api.domain.oauth.dto.response;

public record OAuthLoginResponse(
        Long userId,
        String accessToken,
        String refreshToken
) {}
