package com.semosan.api.domain.tracking.controller.docs;

import com.semosan.api.common.response.ApiResponse;
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
}
