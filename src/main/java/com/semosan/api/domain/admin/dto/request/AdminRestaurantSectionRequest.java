package com.semosan.api.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRestaurantSectionRequest(
        @NotBlank String title
) {
}
