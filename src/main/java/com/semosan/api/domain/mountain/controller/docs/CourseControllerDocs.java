package com.semosan.api.domain.mountain.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.mountain.dto.response.CourseDetailResponse;
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
            summary = "코스 상세 조회 (지도 polyline + 고도 단면 포함)",
            description = "선택한 코스의 메타(거리/시간/난이도) + 지도용 PostGIS polyline(GeoJSON LineString) + "
                    + "점별 고도 배열을 함께 반환합니다. 산림청 GPX 가 없는 코스는 polyline/altitudes 가 null 일 수 있습니다."
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
}
