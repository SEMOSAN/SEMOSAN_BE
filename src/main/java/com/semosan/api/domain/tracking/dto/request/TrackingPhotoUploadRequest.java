package com.semosan.api.domain.tracking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

/**
 * 클라이언트가 MinIO 에 사진을 업로드한 직후, 백엔드에 메타를 통보하는 요청.
 * imageUrl 자체는 #41 presigned URL 흐름에서 얻는다.
 */
public record TrackingPhotoUploadRequest(
        @NotNull(message = "마일스톤 인덱스는 필수입니다.")
        @PositiveOrZero(message = "마일스톤 인덱스는 0 이상이어야 합니다.")
        Integer milestoneIndex,

        @NotNull(message = "마일스톤 기준 거리(m)는 필수입니다.")
        Double milestoneDistanceM,

        @NotBlank(message = "imageUrl 은 필수입니다.")
        String imageUrl,

        @NotNull(message = "촬영 시각은 필수입니다.")
        LocalDateTime capturedAt,

        @NotNull(message = "위도는 필수입니다.")
        Double lat,

        @NotNull(message = "경도는 필수입니다.")
        Double lng,

        Double altitude
) {
}
