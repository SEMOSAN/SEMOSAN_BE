package com.semosan.api.domain.appversion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAppVersionRequest(
        @Valid @NotNull(message = "iOS 버전 정보는 필수입니다.")
        PlatformVersionRequest ios,

        @Valid @NotNull(message = "Android 버전 정보는 필수입니다.")
        PlatformVersionRequest android
) {
    public record PlatformVersionRequest(
            @NotBlank(message = "최신 버전은 필수입니다.")
            String latestVersion,

            @NotBlank(message = "최소 버전은 필수입니다.")
            String minimumVersion,

            boolean forceUpdate,

            String storeUrl,

            String releaseNotes,

            boolean maintenanceMode,

            String maintenanceMessage
    ) {
    }
}
