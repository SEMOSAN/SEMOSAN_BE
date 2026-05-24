package com.semosan.api.domain.tracking.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.tracking.dto.response.LiveActivityCourseResponse;
import com.semosan.api.domain.tracking.dto.response.NearbyMountainResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Tracking", description = "트래킹(등산 시작/진행) 관련 API")
public interface TrackingControllerDocs {

    @Operation(
            summary = "현재 위치 기준 가까운 산과 코스 조회 (트래킹 진입 화면)",
            description = "사용자의 디바이스 GPS 좌표(lat, lng)를 기반으로 가장 가까운 산 1개와 "
                    + "해당 산에 등록된 코스 목록을 반환합니다. 거리 임계값은 두지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "가까운 산 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "산 데이터가 없거나 좌표 컬럼이 비어 있음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<NearbyMountainResponse>> getNearbyMountain(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "현재 위치 위도", required = true)
            @RequestParam @Min(-90) @Max(90) Double lat,
            @Parameter(description = "현재 위치 경도", required = true)
            @RequestParam @Min(-180) @Max(180) Double lng
    );

    @Operation(
            summary = "라이브 액티비티용 코스 정보 조회",
            description = "코스 기반 트래킹의 Live Activity 초기화에 필요한 전체 코스 좌표 배열, "
                    + "전체 거리(m), 예상 소요 시간(분)을 반환합니다. 자유 기록에서는 호출하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "라이브 액티비티용 코스 정보 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "코스 경로 좌표가 등록되지 않음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<LiveActivityCourseResponse>> getLiveActivityCourse(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "코스 ID", required = true)
            @PathVariable Long courseId
    );
}
