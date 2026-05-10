package com.semosan.api.domain.community.post.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record FreePostCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        List<String> imageUrls,
        Integer mainImageIndex
) {
}
