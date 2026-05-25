package com.semosan.api.domain.appversion.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppVersionRequest(
        @NotBlank(message = "최소 버전은 필수입니다.")
        String minimumVersion,

        @NotBlank(message = "최신 버전은 필수입니다.")
        String latestVersion,

        String updateUrl,

        String message
) {
}