package com.semosan.api.domain.semofeed.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 세모피드 생성 요청 DTO.
 * {@code @RequestBody String} 대신 사용하여 Jackson이 JSON 문자열의 따옴표를 올바르게 제거하도록 한다.
 */
public record SemoFeedCreateRequest(
        @NotBlank(message = "imageUrl 은 필수입니다.")
        String imageUrl
) {
}
