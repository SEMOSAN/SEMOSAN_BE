package com.semosan.api.domain.user.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.user.dto.request.RegisterOnboardingRequest;
import com.semosan.api.domain.user.dto.request.UpdateNotificationSettingRequest;
import com.semosan.api.domain.user.dto.request.UpdateUserProfileRequest;
import com.semosan.api.domain.user.dto.response.GetNotificationSettingResponse;
import com.semosan.api.domain.user.dto.response.GetUserProfileResponse;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.Gender;
import com.semosan.api.domain.user.service.UserNotificationSettingService;
import com.semosan.api.domain.user.service.UserOnboardingService;
import com.semosan.api.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserOnboardingService userOnboardingService;

    @Mock
    private UserService userService;

    @Mock
    private UserNotificationSettingService userNotificationSettingService;

    @InjectMocks
    private UserController userController;

    @Test
    void registerUserOnboardingDelegatesAndReturnsSuccessResponse() {
        RegisterOnboardingRequest request = onboardingRequest();

        ResponseEntity<ApiResponse<Void>> response = userController.registerUserOnboarding(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.REGISTER_ONBOARDING_SUCCESS.getHttpStatus());
        verify(userOnboardingService).registerUserOnboarding(1L, request);
    }

    @Test
    void checkNicknameDelegatesAndReturnsSuccessResponse() {
        ResponseEntity<ApiResponse<Void>> response = userController.checkNickname(1L, "닉네임");

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.CHECK_NICKNAME_SUCCESS.getHttpStatus());
        verify(userService).checkNickname(1L, "닉네임");
    }

    @Test
    void updateUserProfileDelegatesAndReturnsSuccessResponse() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "profile.jpg", "닉네임", Gender.MALE, LocalDate.of(1990, 1, 1),
                175.0, 70.0, HikingLevel.BEGINNER, ExerciseType.HIKING
        );

        ResponseEntity<ApiResponse<Void>> response = userController.updateUserProfile(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.UPDATE_PROFILE_SUCCESS.getHttpStatus());
        verify(userService).updateUserProfile(1L, request);
    }

    @Test
    void getUserProfileReturnsSuccessResponse() {
        GetUserProfileResponse profile = new GetUserProfileResponse(
                1L, "profile.jpg", "닉네임", HikingLevel.BEGINNER, Gender.MALE,
                36, 175.0, 70.0, ExerciseType.HIKING, LocalDate.of(1990, 1, 1), "user@example.com"
        );
        when(userOnboardingService.getUserProfile(1L)).thenReturn(profile);

        ResponseEntity<ApiResponse<GetUserProfileResponse>> response = userController.getUserProfile(1L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.GET_PROFILE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(profile);
    }

    @Test
    void notificationSettingEndpointsDelegateAndReturnSuccessResponses() {
        GetNotificationSettingResponse setting = new GetNotificationSettingResponse(true, false, true);
        UpdateNotificationSettingRequest request = new UpdateNotificationSettingRequest(false);
        when(userNotificationSettingService.getNotificationSetting(1L)).thenReturn(setting);

        assertThat(userController.getNotificationSetting(1L).getBody().getData()).isSameAs(setting);
        assertThat(userController.updatePushNotificationSetting(1L, request).getStatusCode())
                .isEqualTo(SuccessStatus.UPDATE_PUSH_NOTIFICATION_SETTING_SUCCESS.getHttpStatus());
        assertThat(userController.updateLiveActivitySetting(1L, request).getStatusCode())
                .isEqualTo(SuccessStatus.UPDATE_LIVE_ACTIVITY_SETTING_SUCCESS.getHttpStatus());
        assertThat(userController.updateVoiceSetting(1L, request).getStatusCode())
                .isEqualTo(SuccessStatus.UPDATE_VOICE_SETTING_SUCCESS.getHttpStatus());
        verify(userNotificationSettingService).updatePushNotificationSetting(1L, request);
        verify(userNotificationSettingService).updateLiveActivitySetting(1L, request);
        verify(userNotificationSettingService).updateVoiceSetting(1L, request);
    }

    private RegisterOnboardingRequest onboardingRequest() {
        return new RegisterOnboardingRequest(
                "닉네임", null, LocalDate.of(1990, 1, 1), Gender.MALE, 175.0, 70.0,
                true, true, true, HikingLevel.BEGINNER, ExerciseType.HIKING,
                ExerciseFrequency.WEEK_1_2, ExerciseDuration.HOUR_1_2
        );
    }
}
