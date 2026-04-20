package com.semosan.api.common.jwt;

public record TokenIssuance(
        String accessToken,
        String refreshToken
) {}
