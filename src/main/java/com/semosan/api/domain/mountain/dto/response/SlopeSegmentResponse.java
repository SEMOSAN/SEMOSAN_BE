package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.enums.SlopeGrade;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 코스 polyline 의 연속된 같은 등급 구간.
 *  - startIdx / endIdx: polyline.coordinates 의 점 인덱스. 둘 다 inclusive.
 *  - grade: 해당 구간의 경사 등급.
 */
@Schema(description = "코스 polyline 의 연속된 같은 경사 등급 구간. 클라이언트는 startIdx~endIdx 범위의 좌표를 grade 별 색으로 그린다.")
public record SlopeSegmentResponse(
        @Schema(description = "polyline.coordinates 의 시작 인덱스 (inclusive)", example = "0")
        int startIdx,

        @Schema(description = "polyline.coordinates 의 끝 인덱스 (inclusive). 다음 segment 의 startIdx 와 같아 끊김 없이 이어진다.",
                example = "12")
        int endIdx,

        @Schema(description = "경사 등급. STEEP_DOWN(심한 내리막) / MILD_DOWN(약한 내리막) / FLAT(평지) / MILD_UP(약한 오르막) / STEEP_UP(심한 오르막).")
        SlopeGrade grade
) {
}
