package com.semosan.api.domain.auth.dto.request;

import com.semosan.api.domain.user.enums.user.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(

        @NotBlank(message = "테스트 유저 ID는 필수입니다.")
        String testUserId,

        @NotNull(message = "디바이스 타입은 필수입니다.")
        DeviceType deviceType,

        @NotBlank(message = "시크릿 키는 필수입니다.")
        String secretKey
) {}
