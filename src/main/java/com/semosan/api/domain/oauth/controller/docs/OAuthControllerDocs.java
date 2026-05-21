package com.semosan.api.domain.oauth.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.oauth.dto.request.OAuthAppleLoginRequest;
import com.semosan.api.domain.oauth.dto.request.OAuthKakaoLoginRequest;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "OAuth", description = "소셜 로그인 관련 API")
public interface OAuthControllerDocs {

    @Operation(
            summary = "카카오 소셜 로그인",
            description = "프론트엔드에서 전달받은 카카오 액세스 토큰(accessToken)으로 로그인 또는 회원가입을 처리하고 서비스 JWT를 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = OAuthLoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (카카오 액세스 토큰 또는 디바이스 타입 누락)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "카카오 API 호출 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<OAuthLoginResponse>> kakaoLogin(
            @Valid @RequestBody OAuthKakaoLoginRequest request
    );

    @Operation(
            summary = "애플 소셜 로그인",
            description = "프론트엔드에서 전달받은 애플 identity token으로 로그인 또는 회원가입을 처리하고 서비스 JWT를 발급합니다. 이름(name)은 최초 로그인 시에만 전달됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = OAuthLoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (identity token 또는 디바이스 타입 누락)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "애플 identity token 검증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "애플 공개키 조회 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<OAuthLoginResponse>> appleLogin(
            @Valid @RequestBody OAuthAppleLoginRequest request
    );

}
