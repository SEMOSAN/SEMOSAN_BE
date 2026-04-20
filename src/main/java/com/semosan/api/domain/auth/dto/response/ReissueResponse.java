package com.semosan.api.domain.auth.dto.response;

public record ReissueResponse(
        String accessToken,
        String refreshToken
) {}
