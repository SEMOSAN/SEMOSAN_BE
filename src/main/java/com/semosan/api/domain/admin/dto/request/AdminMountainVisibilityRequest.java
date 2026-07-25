package com.semosan.api.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminMountainVisibilityRequest(
        @NotNull Boolean isPublic
) {
}
