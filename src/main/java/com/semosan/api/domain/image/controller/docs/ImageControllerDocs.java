package com.semosan.api.domain.image.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.image.dto.response.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Image", description = "이미지 업로드 관련 API")
public interface ImageControllerDocs {

    @Operation(
            summary = "Presigned URL 발급",
            description = "이미지 업로드를 위한 Presigned URL을 발급합니다. 발급된 URL로 PUT 요청하여 직접 업로드합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Presigned URL 발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "이미지 업로드 URL 생성 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @Parameter(description = "버킷명 (reviews, mountains, restaurants)", required = true)
            @RequestParam String bucket,
            @Parameter(description = "원본 파일명 (확장자 추출용)", required = true)
            @RequestParam String filename
    );
}
