package com.semosan.api.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminSemoFeedVisibilityRequest(
        @NotNull Boolean isPublic
) {
}
