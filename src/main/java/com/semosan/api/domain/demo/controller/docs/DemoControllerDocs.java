package com.semosan.api.domain.demo.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Demo", description = "시연용 API")
public interface DemoControllerDocs {

    @Operation(
            summary = "시연용 트래킹 사진 조회",
            description = "MinIO에 미리 올려둔 사진 중 랜덤 N개 + 해당 세션에서 직접 촬영한 사진 URL을 합쳐서 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "시연용 사진 목록 조회 성공"
            )
    })
    ResponseEntity<ApiResponse<List<String>>> getDemoPhotos(
            @Parameter(description = "트래킹 세션 ID", required = true)
            @PathVariable Long sessionId,
            @Parameter(description = "랜덤 사진 개수 (기본값 3)", required = false)
            @RequestParam(defaultValue = "3") int count
    );
}
