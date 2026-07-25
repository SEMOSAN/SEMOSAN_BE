package com.semosan.api.domain.admin.dto.response;

public record AdminLoginResponse(
        Long adminId,
        String name,
        String accessToken
) {}
