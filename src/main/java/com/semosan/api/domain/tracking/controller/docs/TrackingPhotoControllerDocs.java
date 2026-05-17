package com.semosan.api.domain.tracking.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.tracking.dto.request.TrackingPhotoUploadRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingPhotoResponse;
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

import java.util.List;

@Tag(name = "Tracking Photo", description = "트래킹 마일스톤 사진 메타 API")
public interface TrackingPhotoControllerDocs {

    @Operation(
            summary = "트래킹 마일스톤 사진 메타 업로드",
            description = "클라이언트가 MinIO 등 외부 스토리지(#41) 에 사진을 업로드한 직후 호출. "
                    + "본 API 는 이미지 바이너리를 받지 않으며 메타(URL, 좌표, 마일스톤 정보) 만 저장한다. "
                    + "해당 마일스톤에 이미 사진이 있으면 409, 종료된 세션이면 409."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "사진 메타 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 세션 아님",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "중복 마일스톤 또는 종료된 세션",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ResponseEntity<ApiResponse<TrackingPhotoResponse>> upload(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "트래킹 세션 ID", required = true) @PathVariable Long sessionId,
            @Valid @RequestBody TrackingPhotoUploadRequest request
    );

    @Operation(summary = "트래킹 세션의 사진 목록 조회",
            description = "본인 소유 세션의 마일스톤 사진을 milestone_index 오름차순으로 반환합니다.")
    ResponseEntity<ApiResponse<List<TrackingPhotoResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "트래킹 세션 ID", required = true) @PathVariable Long sessionId
    );
}
