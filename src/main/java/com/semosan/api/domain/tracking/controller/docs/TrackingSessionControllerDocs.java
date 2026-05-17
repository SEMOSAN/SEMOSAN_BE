package com.semosan.api.domain.tracking.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.tracking.dto.request.CreateTrackingSessionRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Tracking Session", description = "트래킹 세션 관리 API")
public interface TrackingSessionControllerDocs {

    @Operation(
            summary = "트래킹 세션 시작",
            description = "산/코스 정보로 새 트래킹 세션을 생성합니다. 자유 기록(isFreeRecording=true)이면 courseId 는 무시됩니다. "
                    + "유저당 진행 중 세션이 이미 있으면 409 응답."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "세션 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "산 또는 코스 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 진행 중인 세션 존재",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ResponseEntity<ApiResponse<TrackingSessionResponse>> createSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTrackingSessionRequest request
    );

    @Operation(summary = "현재 진행 중 트래킹 세션 조회",
            description = "앱 재진입 시 호출. 진행 중(IN_PROGRESS/PAUSED) 세션이 없으면 data 가 비어 응답됩니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ResponseEntity<ApiResponse<TrackingSessionResponse>> getActiveSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    );

    @Operation(summary = "트래킹 세션 상세 조회", description = "본인 소유 세션만 조회 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 세션 아님",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ResponseEntity<ApiResponse<TrackingSessionResponse>> getSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "세션 ID", required = true) @PathVariable Long sessionId
    );

    @Operation(summary = "트래킹 세션 일시정지", description = "IN_PROGRESS 상태에서만 가능. 점 수집은 멈추고 duration 에서 제외됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일시정지 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "현재 상태에서 일시정지 불가",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ResponseEntity<ApiResponse<TrackingSessionResponse>> pauseSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    );

    @Operation(summary = "트래킹 세션 재개", description = "PAUSED 상태에서만 가능. 일시정지 누적 시간이 합산됩니다.")
    ResponseEntity<ApiResponse<TrackingSessionResponse>> resumeSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    );

    @Operation(summary = "트래킹 세션 정상 종료",
            description = "세션 상태/시각만 마감합니다. 실제 HikingRecord 변환은 #20 에서 구현됩니다.")
    ResponseEntity<ApiResponse<TrackingSessionResponse>> completeSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    );

    @Operation(summary = "트래킹 세션 포기", description = "기록 없이 세션을 ABANDONED 처리합니다.")
    ResponseEntity<ApiResponse<TrackingSessionResponse>> abandonSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    );
}
