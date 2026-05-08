package com.semosan.api.domain.notification.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.notification.dto.FcmTokenDeleteRequest;
import com.semosan.api.domain.notification.dto.FcmTokenRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "FCM Token", description = "FCM 토큰 관리 API")
public interface FcmTokenControllerDocs {

    @Operation(summary = "FCM 토큰 등록", description = "클라이언트 FCM 토큰을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FCM 토큰 등록 성공")
    })
    ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRegisterRequest request
    );

    @Operation(summary = "FCM 토큰 삭제", description = "로그아웃 시 FCM 토큰을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FCM 토큰 삭제 성공")
    })
    ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenDeleteRequest request
    );
}
