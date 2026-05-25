package com.semosan.api.domain.appversion.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.appversion.dto.request.UpdateAppVersionRequest;
import com.semosan.api.domain.appversion.dto.response.AppVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "App Version", description = "앱 버전 관리 API")
public interface AppVersionControllerDocs {

    @Operation(
            summary = "앱 버전 정보 조회",
            description = "현재 설정된 앱 최소 버전 및 최신 버전 정보를 조회합니다. 인증 불필요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "앱 버전 정보 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "버전 정보 파일이 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<AppVersionResponse>> getAppVersion();

    @Operation(
            summary = "앱 버전 정보 수정",
            description = "앱 최소 버전 및 최신 버전 정보를 수정합니다. 인증 필요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "앱 버전 정보 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "버전 정보 저장 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<AppVersionResponse>> updateAppVersion(
            @RequestBody UpdateAppVersionRequest request
    );
}