package com.semosan.api.domain.hiking.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingMountainRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Hiking Record", description = "등산 기록 관련 API")
public interface HikingRecordControllerDocs {

    @Operation(
            summary = "나의 등산 기록 목록 조회",
            description = "사용자가 참여한 등산 기록을 기록 단위로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "나의 등산 기록 목록 조회 성공"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<GetUserHikingRecordResponse>>> getUserHikingRecords(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "내가 다녀온 산 목록 조회",
            description = "사용자가 등산 기록을 남긴 산 목록을 산 단위로 묶어 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내가 다녀온 산 목록 조회 성공"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<GetUserHikingMountainRecordResponse>>> getUserHikingMountainRecords(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "특정 산의 나의 등산 기록 목록 조회",
            description = "지정된 산에 대해 사용자가 다녀온 등산 기록을 기록(코스) 단위로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "특정 산의 나의 등산 기록 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "산을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<PageResponse<GetUserHikingRecordResponse>>> getUserHikingRecordsByMountainId(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "산 ID", required = true)
            @PathVariable Long mountainId,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "나의 등산 기록 요약 조회",
            description = "마이페이지에서 사용하는 누적 등산 횟수, 정복한 산 수, 누적 등산 고도를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "나의 등산 기록 요약 조회 성공"
            )
    })
    ResponseEntity<ApiResponse<GetUserHikingRecordSummaryResponse>> getUserHikingRecordSummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    );
}
