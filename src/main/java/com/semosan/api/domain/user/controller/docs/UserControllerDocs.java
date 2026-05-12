package com.semosan.api.domain.user.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.user.dto.request.RegisterOnboardingRequest;
import com.semosan.api.domain.user.dto.request.UpdateNotificationSettingRequest;
import com.semosan.api.domain.user.dto.request.UpdateUserProfileRequest;
import com.semosan.api.domain.user.dto.response.GetNotificationSettingResponse;
import com.semosan.api.domain.user.dto.response.GetUserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User", description = "유저 관련 API")
public interface UserControllerDocs {

    @Operation(
            summary = "온보딩 등록",
            description = "로그인한 사용자의 온보딩 정보를 최초 1회 등록합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "온보딩 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "온보딩 요청 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 온보딩을 완료한 사용자",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> registerUserOnboarding(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegisterOnboardingRequest request
    );

    @Operation(
            summary = "닉네임 사용 가능 여부 조회",
            description = "로그인한 사용자가 온보딩 입력 과정에서 닉네임 형식, 금칙어, 사칭 표현, 중복 여부를 확인합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "닉네임 사용 가능 여부 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> checkNickname(
            @AuthenticationPrincipal Long userId,
            @RequestParam String nickname
    );

    @Operation(
            summary = "프로필 수정",
            description = "로그인한 사용자의 프로필 정보와 프로필 화면의 온보딩 항목을 수정합니다. 요청에 포함된 값만 변경됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "프로필 수정 요청 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> updateUserProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request
    );

    @Operation(
            summary = "프로필 조회",
            description = "로그인한 사용자의 프로필 수정 화면에 필요한 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<GetUserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal Long userId
    );

    @Operation(
            summary = "알림 설정 조회",
            description = "로그인한 사용자의 푸시알림, 라이브 액티비티, 음성 설정을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "알림 설정 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 알림 설정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<GetNotificationSettingResponse>> getNotificationSetting(
            @AuthenticationPrincipal Long userId
    );

    @Operation(
            summary = "푸시알림 설정 변경",
            description = "로그인한 사용자의 푸시알림 on/off 설정을 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "푸시알림 설정 변경 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "푸시알림 설정 요청 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> updatePushNotificationSetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    );

    @Operation(
            summary = "라이브 액티비티 설정 변경",
            description = "로그인한 사용자의 라이브 액티비티 on/off 설정을 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "라이브 액티비티 설정 변경 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "라이브 액티비티 설정 요청 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 알림 설정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> updateLiveActivitySetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    );

    @Operation(
            summary = "음성 설정 변경",
            description = "로그인한 사용자의 음성 on/off 설정을 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "음성 설정 변경 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "음성 설정 요청 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 알림 설정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> updateVoiceSetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    );
}
