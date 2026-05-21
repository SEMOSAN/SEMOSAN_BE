package com.semosan.api.domain.image.dto.response;

public record PresignedUrlResponse(
        String uploadUrl,
        String imageUrl
) {
}
