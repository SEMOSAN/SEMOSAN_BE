package com.semosan.api.domain.community.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FreePostCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String content,
        List<String> imageUrls,
        Integer mainImageIndex
) {
}
