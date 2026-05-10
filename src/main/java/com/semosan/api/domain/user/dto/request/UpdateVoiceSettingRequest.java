package com.semosan.api.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateVoiceSettingRequest(
        @NotNull
        Boolean enabled
) {
}
