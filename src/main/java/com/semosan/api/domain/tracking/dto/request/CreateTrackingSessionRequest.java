package com.semosan.api.domain.tracking.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateTrackingSessionRequest(
        @NotNull(message = "산 ID는 필수입니다.")
        Long mountainId,

        /** 자유 기록(코스 미선택) 인 경우 null */
        Long courseId,

        @NotNull(message = "자유 기록 여부는 필수입니다.")
        Boolean isFreeRecording
) {
}
