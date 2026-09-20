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
                    + "해당 산의 코스 목록(ID 오름차순), 산 선택용 nearbyMountains(mountainId, name)를 반환합니다. "
                    + "기본 mountain은 거리 제한 없이 선택합니다. nearbyMountains는 사용자 좌표에서 "
                    + "산 대표 좌표까지 구면 거리 2,000m 이내인 공개 산만 포함하며, 가까운 순·동일 거리면 산 ID순입니다. "
                    + "기본 산도 반경 안이면 포함하고, 코스가 없는 산도 포함합니다. "
                    + "반경 내 산이 없으면 nearbyMountains는 빈 배열이며 기존 mountain과 courses는 유지합니다. "
                    + "산 변경 시 GET /api/mountains/{mountainId}, 코스 선택 시 GET /api/courses/{courseId}를 호출합니다."
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
                    + "전체 거리(m), 예상 소요 시간(분)을 반환합니다. 자유 기록에서는 호출하지 않습니다.\n\n"
                    + "`summitDistance`(시작점→정상 누적 거리, m)와 `summitEstimatedTime`(정상까지 예상 시간, 분)이 "
                    + "함께 내려갑니다. 사진 마일스톤 푸시가 이 거리 기준으로 발송되므로 "
                    + "\"정상까지 거리/시간\" 표시에는 totalDistance 를 절반으로 나누지 말고 이 값을 쓰세요. "
                    + "정상 좌표가 없어 계산이 불가한 코스는 두 필드 모두 null 입니다."
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
