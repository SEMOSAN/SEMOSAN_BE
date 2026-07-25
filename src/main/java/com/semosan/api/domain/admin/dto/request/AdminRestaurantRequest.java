package com.semosan.api.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRestaurantRequest(
        @NotBlank String name,
        String category,
        String menu,
        String description,
        String imageUrl,
        String mapUrl,
        String blogUrl
) {
}
