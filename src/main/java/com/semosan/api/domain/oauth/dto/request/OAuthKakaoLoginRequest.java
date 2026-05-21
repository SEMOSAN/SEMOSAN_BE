package com.semosan.api.domain.oauth.dto.request;

import com.semosan.api.domain.user.enums.user.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthKakaoLoginRequest(

        @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
        String accessToken,

        @NotNull(message = "디바이스 타입은 필수입니다.")
        DeviceType deviceType
) {}
