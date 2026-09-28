package com.semosan.api.domain.hiking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHikingRecordNameRequest(

        @Schema(description = "새 기록 이름", example = "북한산 아침 산책")
        @NotBlank(message = "기록 이름은 비워둘 수 없습니다.")
        @Size(max = 30, message = "기록 이름은 30자 이하여야 합니다.")
        String name
) {
}
