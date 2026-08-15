package com.semosan.api.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminMountainSummitRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        Double altitude
) {
}
