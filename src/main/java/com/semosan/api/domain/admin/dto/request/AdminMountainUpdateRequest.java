package com.semosan.api.domain.admin.dto.request;

import com.semosan.api.domain.mountain.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminMountainUpdateRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotNull Double altitude,
        @NotNull Difficulty difficulty,
        Integer duration,
        List<String> imageUrls
) {
}
