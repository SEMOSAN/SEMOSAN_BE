package com.semosan.api.domain.admin.dto.request;

import com.semosan.api.domain.mountain.enums.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 관리자가 지도에서 찍은 좌표로 코스를 만드는 요청.
 *
 * distance 는 클라이언트가 보내지 않는다 — 서버가 points 로 직접 계산한다.
 * 트래킹 누적 거리와 같은 공식으로 구해야 마일스톤 푸시 지점이 어긋나지 않기 때문이다.
 */
public record AdminCourseCreateRequest(

        @NotNull(message = "산 ID 는 필수입니다.")
        Long mountainId,

        @NotBlank(message = "코스 이름은 필수입니다.")
        @Size(max = 100, message = "코스 이름은 100자 이하여야 합니다.")
        String name,

        @NotNull(message = "난이도는 필수입니다.")
        Difficulty difficulty,

        @NotNull(message = "소요 시간(분)은 필수입니다.")
        @Positive(message = "소요 시간은 1분 이상이어야 합니다.")
        Integer duration,

        @NotEmpty(message = "경로 좌표는 필수입니다.")
        @Size(min = 2, max = 500, message = "경로 좌표는 2개 이상 500개 이하여야 합니다.")
        List<@Valid PointRequest> points,

        @Schema(description = "정상으로 지정할 좌표의 인덱스(0-based). 비우면 정상 미지정 — 마일스톤이 거리 4등분으로 fallback 된다.")
        Integer summitPointIndex,

        @Size(max = 100, message = "시작 지점명은 100자 이하여야 합니다.")
        String startName,

        @Size(max = 100, message = "종료 지점명은 100자 이하여야 합니다.")
        String endName
) {

    public record PointRequest(

            @NotNull(message = "위도는 필수입니다.")
            @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
            Double lat,

            @NotNull(message = "경도는 필수입니다.")
            @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
            Double lng
    ) {
    }
}
