package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.dto.request.RegisterOnboardingRequest;
import com.semosan.api.domain.user.dto.response.GetUserProfileResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserNotificationSetting;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.Gender;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import com.semosan.api.domain.user.policy.NicknamePolicy;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOnboardingServiceTest {

    @Mock
    private UserOnboardingRepository userOnboardingRepository;

    @Mock
    private UserNotificationSettingRepository userNotificationSettingRepository;

    @Mock
    private NicknamePolicy nicknamePolicy;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private UserOnboardingService service;

    @Test
    void registerUserOnboardingCompletesProfileAndCreatesOnboarding() {
        User user = user(1L);
        UserNotificationSetting setting = UserNotificationSetting.createDefault(user);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.existsByUser_Id(1L)).thenReturn(false);
        when(userNotificationSettingRepository.findByUser_Id(1L)).thenReturn(Optional.of(setting));
        ArgumentCaptor<UserOnboarding> onboardingCaptor = ArgumentCaptor.forClass(UserOnboarding.class);

        service.registerUserOnboarding(1L, request());

        assertThat(user.getOnboardingStatus()).isEqualTo(OnboardingStatus.COMPLETE);
        assertThat(user.getNickname()).isEqualTo("푸름");
        assertThat(setting.isPushNotificationEnabled()).isTrue();
        assertThat(setting.isLiveActivityEnabled()).isFalse();
        assertThat(setting.isVoiceEnabled()).isTrue();
        verify(nicknamePolicy).validate("푸름");
        verify(userOnboardingRepository).save(onboardingCaptor.capture());
        assertThat(onboardingCaptor.getValue().getUser()).isSameAs(user);
        assertThat(onboardingCaptor.getValue().getHikingLevel()).isEqualTo(HikingLevel.BEGINNER);
    }

    @Test
    void registerThrowsWhenOnboardingAlreadyExists() {
        User user = user(1L);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.existsByUser_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.registerUserOnboarding(1L, request()))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ONBOARDING_ALREADY_COMPLETED);
        verify(nicknamePolicy, never()).validate(any());
    }

    @Test
    void registerThrowsWhenUnderAge() {
        User user = user(1L);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.existsByUser_Id(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.registerUserOnboarding(
                1L,
                request(LocalDate.now().minusYears(13), ExerciseType.RUNNING, ExerciseFrequency.WEEK_1_2, ExerciseDuration.UNDER_1H)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.UNDER_AGE_NOT_ALLOWED);
    }

    @Test
    void registerThrowsWhenExerciseDetailIsNotAllowed() {
        User user = user(1L);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.existsByUser_Id(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.registerUserOnboarding(
                1L,
                request(LocalDate.now().minusYears(20), ExerciseType.NONE, ExerciseFrequency.WEEK_1_2, null)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.EXERCISE_DETAIL_NOT_ALLOWED);
    }

    @Test
    void registerThrowsWhenExerciseDetailIsRequired() {
        User user = user(1L);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.existsByUser_Id(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.registerUserOnboarding(
                1L,
                request(LocalDate.now().minusYears(20), ExerciseType.RUNNING, null, ExerciseDuration.UNDER_1H)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.EXERCISE_DETAIL_REQUIRED);
    }

    @Test
    void registerThrowsWhenNotificationSettingDoesNotExist() {
        User user = user(1L);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.existsByUser_Id(1L)).thenReturn(false);
        when(userNotificationSettingRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerUserOnboarding(1L, request()))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_SETTING_NOT_FOUND);
    }

    @Test
    void registerWrapsDuplicateSaveAsAlreadyCompleted() {
        User user = user(1L);
        UserNotificationSetting setting = UserNotificationSetting.createDefault(user);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.existsByUser_Id(1L)).thenReturn(false);
        when(userNotificationSettingRepository.findByUser_Id(1L)).thenReturn(Optional.of(setting));
        when(userOnboardingRepository.save(any(UserOnboarding.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.registerUserOnboarding(1L, request()))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ONBOARDING_ALREADY_COMPLETED);
    }

    @Test
    void getUserProfileReturnsUserAndOnboardingFields() {
        User user = user(1L);
        user.completeOnboarding(new com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand(
                "푸름",
                "https://example.com/profile.png",
                LocalDate.now().minusYears(20),
                Gender.FEMALE,
                170.0,
                60.0
        ));
        UserOnboarding onboarding = UserOnboarding.create(new com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand(
                user,
                HikingLevel.BEGINNER,
                ExerciseType.RUNNING,
                ExerciseFrequency.WEEK_1_2,
                ExerciseDuration.UNDER_1H
        ));
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.findByUser_Id(1L)).thenReturn(Optional.of(onboarding));

        GetUserProfileResponse response = service.getUserProfile(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("푸름");
        assertThat(response.hikingLevel()).isEqualTo(HikingLevel.BEGINNER);
        assertThat(response.exerciseType()).isEqualTo(ExerciseType.RUNNING);
    }

    @Test
    void getUserProfileReturnsNullOnboardingFieldsWhenOnboardingMissing() {
        User user = user(1L);
        user.completeOnboarding(new com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand(
                "푸름",
                "https://example.com/profile.png",
                LocalDate.now().minusYears(20),
                Gender.FEMALE,
                170.0,
                60.0
        ));
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        GetUserProfileResponse response = service.getUserProfile(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("푸름");
        assertThat(response.hikingLevel()).isNull();
        assertThat(response.exerciseType()).isNull();
    }

    private RegisterOnboardingRequest request() {
        return request(LocalDate.now().minusYears(20), ExerciseType.RUNNING, ExerciseFrequency.WEEK_1_2, ExerciseDuration.UNDER_1H);
    }

    private RegisterOnboardingRequest request(
            LocalDate birthDate,
            ExerciseType exerciseType,
            ExerciseFrequency frequency,
            ExerciseDuration duration
    ) {
        return new RegisterOnboardingRequest(
                "푸름",
                "https://example.com/profile.png",
                birthDate,
                Gender.FEMALE,
                170.0,
                60.0,
                true,
                false,
                true,
                HikingLevel.BEGINNER,
                exerciseType,
                frequency,
                duration
        );
    }

    private User user(Long id) {
        User user = User.createTestUser("test-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
