package com.semosan.api.domain.mountain.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.mountain.dto.response.CourseDetailResponse;
import com.semosan.api.domain.mountain.dto.response.CourseLikeToggleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Course", description = "등산 코스 관련 API")
public interface CourseControllerDocs {

    @Operation(
            summary = "코스 상세 조회 (지도 polyline + 고도 단면 + 경사 등급 segments 포함)",
            description = "선택한 코스의 메타(거리/시간/난이도/누적 상승·하강) + 지도용 PostGIS polyline(GeoJSON LineString) + "
                    + "점별 고도 배열 + 경사 등급별로 묶인 segments 배열을 함께 반환합니다. "
                    + "segments 는 polyline.coordinates 의 연속된 같은 등급(STEEP_DOWN/MILD_DOWN/FLAT/MILD_UP/STEEP_UP) 구간을 "
                    + "startIdx~endIdx(둘 다 inclusive) 로 표시하며, 클라이언트는 해당 인덱스 범위의 좌표를 등급별 색으로 그릴 수 있습니다. "
                    + "산림청 GPX 가 없는 코스는 polyline/altitudes 가 null 이고 segments 는 빈 배열일 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 상세 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음 (MTN_404_3)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<CourseDetailResponse>> getCourseDetail(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "코스 ID", required = true)
            @PathVariable Long courseId
    );

    @Operation(
            summary = "코스 좋아요 토글",
            description = "로그인한 사용자가 코스 좋아요를 누르거나 취소합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 좋아요 토글 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음 (MTN_404_3)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<CourseLikeToggleResponse>> toggleCourseLike(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "코스 ID", required = true)
            @PathVariable Long courseId
    );
}
