package com.semosan.api.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PresignedUrlResponse(
        String uploadUrl,
        String imageUrl,

        @Schema(description = "업로드 시 반드시 이 값으로 Content-Type 헤더를 보내야 한다. 서명에 포함되어 있어 값이 다르면 업로드가 거부된다.",
                example = "image/jpeg")
        String contentType
) {
}
