package com.semosan.api.domain.user.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.user.controller.docs.UserControllerDocs;
import com.semosan.api.domain.user.dto.request.RegisterOnboardingRequest;
import com.semosan.api.domain.user.dto.request.UpdateNotificationSettingRequest;
import com.semosan.api.domain.user.dto.request.UpdateUserProfileRequest;
import com.semosan.api.domain.user.dto.response.GetNotificationSettingResponse;
import com.semosan.api.domain.user.dto.response.GetUserProfileResponse;
import com.semosan.api.domain.user.service.UserNotificationSettingService;
import com.semosan.api.domain.user.service.UserOnboardingService;
import com.semosan.api.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserOnboardingService userOnboardingService;
    private final UserService userService;
    private final UserNotificationSettingService userNotificationSettingService;

    // 로그인한 사용자의 온보딩 정보를 등록합니다.
    @PostMapping("/onboarding")
    @Override
    public ResponseEntity<ApiResponse<Void>> registerUserOnboarding(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegisterOnboardingRequest request
    ) {
        userOnboardingService.registerUserOnboarding(userId, request);
        return ApiResponse.success(SuccessStatus.REGISTER_ONBOARDING_SUCCESS);
    }

    // 로그인한 사용자의 프로필 정보를 수정합니다.
    @PatchMapping("/profile")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateUserProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        userService.updateUserProfile(userId, request);
        return ApiResponse.success(SuccessStatus.UPDATE_PROFILE_SUCCESS);
    }

    // 로그인한 사용자의 프로필 정보를 조회합니다.
    @GetMapping("/profile")
    @Override
    public ResponseEntity<ApiResponse<GetUserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal Long userId
    ) {
        GetUserProfileResponse response = userOnboardingService.getUserProfile(userId);
        return ApiResponse.success(SuccessStatus.GET_PROFILE_SUCCESS, response);
    }

    // 로그인한 사용자의 알림 설정을 조회합니다.
    @GetMapping("/notification-settings")
    @Override
    public ResponseEntity<ApiResponse<GetNotificationSettingResponse>> getNotificationSetting(
            @AuthenticationPrincipal Long userId
    ) {
        GetNotificationSettingResponse response = userNotificationSettingService.getNotificationSetting(userId);
        return ApiResponse.success(SuccessStatus.GET_NOTIFICATION_SETTING_SUCCESS, response);
    }

    // 로그인한 사용자의 푸시알림 설정을 변경합니다.
    @PatchMapping("/notification-settings/push")
    @Override
    public ResponseEntity<ApiResponse<Void>> updatePushNotificationSetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        userNotificationSettingService.updatePushNotificationSetting(userId, request);
        return ApiResponse.success(SuccessStatus.UPDATE_PUSH_NOTIFICATION_SETTING_SUCCESS);
    }

    // 로그인한 사용자의 라이브 액티비티 설정을 변경합니다.
    @PatchMapping("/notification-settings/live-activity")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateLiveActivitySetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        userNotificationSettingService.updateLiveActivitySetting(userId, request);
        return ApiResponse.success(SuccessStatus.UPDATE_LIVE_ACTIVITY_SETTING_SUCCESS);
    }

    // 로그인한 사용자의 음성 설정을 변경합니다.
    @PatchMapping("/notification-settings/voice")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateVoiceSetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        userNotificationSettingService.updateVoiceSetting(userId, request);
        return ApiResponse.success(SuccessStatus.UPDATE_VOICE_SETTING_SUCCESS);
    }
}
