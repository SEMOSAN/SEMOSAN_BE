package com.semosan.api.domain.oauth.dto.request;

import com.semosan.api.domain.user.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthAppleLoginRequest(

        @NotBlank(message = "identity token은 필수입니다.")
        String identityToken,

        String name,

        @NotNull(message = "디바이스 타입은 필수입니다.")
        DeviceType deviceType
) {}
