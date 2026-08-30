package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.hiking.repository.CourseDifficultyFeedbackRepository;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.notification.repository.NotificationRepository;
import com.semosan.api.domain.review.repository.ReviewRepository;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.dto.command.OAuthUserProfile;
import com.semosan.api.domain.user.dto.request.UpdateUserProfileRequest;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import com.semosan.api.domain.user.policy.DefaultNicknameGenerator;
import com.semosan.api.domain.user.policy.NicknamePolicy;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserNotificationSettingRepository userNotificationSettingRepository;

    @Mock
    private UserOnboardingRepository userOnboardingRepository;

    @Mock
    private MountainLikeRepository mountainLikeRepository;

    @Mock
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private HikingMemberRepository hikingMemberRepository;

    @Mock
    private HikingRecordRepository hikingRecordRepository;

    @Mock
    private CourseDifficultyFeedbackRepository courseDifficultyFeedbackRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NicknamePolicy nicknamePolicy;

    @Mock
    private DefaultNicknameGenerator defaultNicknameGenerator;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private UserService userService;

    @Test
    void findOrRegisterOAuthUserReturnsActiveUserWhenExists() {
        User user = User.createOAuthUser("kakao-id", "user@example.com", "name", DeviceType.IOS, OAuthProvider.KAKAO);
        when(userRepository.findByOauthIdAndOauthProvider("kakao-id", OAuthProvider.KAKAO))
                .thenReturn(Optional.of(user));

        User result = userService.findOrRegisterOAuthUser(
                new OAuthUserProfile("kakao-id", "new@example.com", "new-name"),
                OAuthProvider.KAKAO,
                DeviceType.ANDROID
        );

        assertThat(result).isSameAs(user);
    }

    @Test
    void findOrRegisterOAuthUserCreatesNewUserWhenActiveUserDoesNotExist() {
        when(userRepository.findByOauthIdAndOauthProvider("kakao-id", OAuthProvider.KAKAO))
                .thenReturn(Optional.empty());
        when(defaultNicknameGenerator.generate()).thenReturn("용감한등산러1234");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrRegisterOAuthUser(
                new OAuthUserProfile("kakao-id", "user@example.com", "name"),
                OAuthProvider.KAKAO,
                DeviceType.IOS
        );

        verify(userRepository).save(result);
        verify(userNotificationSettingRepository).save(any());
        assertThat(result.getOauthId()).isEqualTo("kakao-id");
        assertThat(result.getOauthProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.isDeleted()).isFalse();
        assertThat(result.getName()).isEqualTo("name");
        assertThat(result.getNickname()).isEqualTo("용감한등산러1234");
    }

    @Test
    void findOrRegisterOAuthUserCreatesNewUserWhenMatchedUserIsDeleted() {
        User deletedUser = User.createOAuthUser("kakao-id", "old@example.com", "old-name", DeviceType.IOS, OAuthProvider.KAKAO);
        ReflectionTestUtils.setField(deletedUser, "id", 1L);
        deletedUser.withdraw();
        ReflectionTestUtils.setField(deletedUser, "oauthId", "kakao-id");

        when(userRepository.findByOauthIdAndOauthProvider("kakao-id", OAuthProvider.KAKAO))
                .thenReturn(Optional.of(deletedUser));
        when(defaultNicknameGenerator.generate()).thenReturn("씩씩한산악인5391");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrRegisterOAuthUser(
                new OAuthUserProfile("kakao-id", "user@example.com", "name"),
                OAuthProvider.KAKAO,
                DeviceType.IOS
        );

        verify(userRepository).save(result);
        verify(userNotificationSettingRepository).save(any());
        assertThat(result).isNotSameAs(deletedUser);
        assertThat(result.isDeleted()).isFalse();
        assertThat(result.getName()).isEqualTo("name");
        assertThat(result.getNickname()).isEqualTo("씩씩한산악인5391");
    }

    @Test
    void findOrRegisterOAuthUserCreatesAppleUserWithGeneratedNickname() {
        when(userRepository.findByOauthIdAndOauthProvider("apple-id", OAuthProvider.APPLE))
                .thenReturn(Optional.empty());
        when(defaultNicknameGenerator.generate()).thenReturn("빠른하이커1204");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrRegisterOAuthUser(
                new OAuthUserProfile("apple-id", "user@example.com", "apple-name"),
                OAuthProvider.APPLE,
                DeviceType.IOS
        );

        verify(userRepository).save(result);
        verify(userNotificationSettingRepository).save(any());
        assertThat(result.getOauthId()).isEqualTo("apple-id");
        assertThat(result.getOauthProvider()).isEqualTo(OAuthProvider.APPLE);
        assertThat(result.getName()).isEqualTo("apple-name");
        assertThat(result.getNickname()).isEqualTo("빠른하이커1204");
    }

    @Test
    void findOrCreateTestUserReturnsActiveUserWhenExists() {
        User user = User.createTestUser("test-id", DeviceType.IOS);
        when(userRepository.findByOauthIdAndOauthProvider("test-id", OAuthProvider.TEST))
                .thenReturn(Optional.of(user));

        User result = userService.findOrCreateTestUser("test-id", DeviceType.ANDROID);

        assertThat(result).isSameAs(user);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateTestUserCreatesNewUserWhenMissing() {
        when(userRepository.findByOauthIdAndOauthProvider("test-id", OAuthProvider.TEST))
                .thenReturn(Optional.empty());
        when(defaultNicknameGenerator.generate()).thenReturn("테스트닉네임1234");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrCreateTestUser("test-id", DeviceType.ANDROID);

        assertThat(result.getOauthId()).isEqualTo("test-id");
        assertThat(result.getOauthProvider()).isEqualTo(OAuthProvider.TEST);
        assertThat(result.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(result.getNickname()).isEqualTo("테스트닉네임1234");
        verify(userNotificationSettingRepository).save(any());
    }

    @Test
    void findOrCreateTestUserRejectsOfficialAccountOauthId() {
        assertThatThrownBy(() -> userService.findOrCreateTestUser("semosan_official", DeviceType.IOS))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.FORBIDDEN);

        verify(userRepository, never()).findByOauthIdAndOauthProvider(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void checkNicknameValidatesAfterActiveUserLookup() {
        User user = User.createTestUser("nickname-check-user", DeviceType.IOS);
        when(userReader.findActiveUserById(1L)).thenReturn(user);

        userService.checkNickname(1L, "푸름");

        verify(nicknamePolicy).validate("푸름");
    }

    @Test
    void updateUserProfileThrowsWhenNoFieldsProvided() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> userService.updateUserProfile(1L, request))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.PROFILE_UPDATE_FIELD_REQUIRED);
        verify(userReader, never()).findActiveUserById(1L);
    }

    @Test
    void updateUserProfileValidatesNicknameAndDoesNotTouchOnboardingWhenOnlyProfileFieldsProvided() {
        User user = User.createTestUser("profile-test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "profile.jpg",
                "새닉네임",
                null,
                null,
                180.0,
                null,
                null,
                null
        );
        when(userReader.findActiveUserById(1L)).thenReturn(user);

        userService.updateUserProfile(1L, request);

        verify(nicknamePolicy).validate("새닉네임");
        assertThat(user.getProfileUrl()).isEqualTo("profile.jpg");
        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getHeight()).isEqualTo(180.0);
        verify(userOnboardingRepository, never()).findByUser_Id(1L);
    }

    @Test
    void updateUserProfileDoesNotValidateNicknameWhenNicknameIsNull() {
        User user = User.createTestUser("profile-test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "profile.jpg",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(userReader.findActiveUserById(1L)).thenReturn(user);

        userService.updateUserProfile(1L, request);

        verify(nicknamePolicy, never()).validate(any());
        assertThat(user.getProfileUrl()).isEqualTo("profile.jpg");
    }

    @Test
    void updateUserProfileCreatesOnboardingWhenOneOnboardingFieldProvidedAndRowMissing() {
        User user = User.createTestUser("profile-test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                HikingLevel.BEGINNER,
                null
        );
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(userOnboardingRepository.save(any(UserOnboarding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUserProfile(1L, request);

        var captor = forClass(UserOnboarding.class);
        verify(userOnboardingRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getHikingLevel()).isEqualTo(HikingLevel.BEGINNER);
        assertThat(captor.getValue().getExerciseType()).isNull();
    }

    @Test
    void updateUserProfileCreatesOnboardingWithExerciseTypeWhenRowMissing() {
        User user = User.createTestUser("profile-test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ExerciseType.HIKING
        );
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(userOnboardingRepository.save(any(UserOnboarding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUserProfile(1L, request);

        var captor = forClass(UserOnboarding.class);
        verify(userOnboardingRepository).save(captor.capture());
        assertThat(captor.getValue().getHikingLevel()).isNull();
        assertThat(captor.getValue().getExerciseType()).isEqualTo(ExerciseType.HIKING);
    }

    @Test
    void updateUserProfileUpdatesExistingOnboardingWithProvidedFieldsOnly() {
        User user = User.createTestUser("profile-test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        UserOnboarding onboarding = UserOnboarding.create(new CreateUserOnboardingCommand(
                user,
                HikingLevel.EXPERIENCED,
                ExerciseType.HIKING,
                null,
                null
        ));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ExerciseType.NONE
        );
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.findByUser_Id(1L)).thenReturn(Optional.of(onboarding));

        userService.updateUserProfile(1L, request);

        assertThat(onboarding.getHikingLevel()).isEqualTo(HikingLevel.EXPERIENCED);
        assertThat(onboarding.getExerciseType()).isEqualTo(ExerciseType.NONE);
    }

    @Test
    void updateUserProfileUpdatesExistingOnboardingHikingLevelOnly() {
        User user = User.createTestUser("profile-test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        UserOnboarding onboarding = UserOnboarding.create(new CreateUserOnboardingCommand(
                user,
                HikingLevel.BEGINNER,
                ExerciseType.HIKING,
                null,
                null
        ));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                HikingLevel.EXPERT,
                null
        );
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(userOnboardingRepository.findByUser_Id(1L)).thenReturn(Optional.of(onboarding));

        userService.updateUserProfile(1L, request);

        assertThat(onboarding.getHikingLevel()).isEqualTo(HikingLevel.EXPERT);
        assertThat(onboarding.getExerciseType()).isEqualTo(ExerciseType.HIKING);
    }

    @Test
    void findOrCreateTestUserCreatesNewUserWhenMatchedUserIsDeleted() {
        User deletedUser = User.createTestUser("test-id", DeviceType.IOS);
        ReflectionTestUtils.setField(deletedUser, "id", 1L);
        deletedUser.withdraw();
        ReflectionTestUtils.setField(deletedUser, "oauthId", "test-id");

        when(userRepository.findByOauthIdAndOauthProvider("test-id", OAuthProvider.TEST))
                .thenReturn(Optional.of(deletedUser));
        when(defaultNicknameGenerator.generate()).thenReturn("재가입테스트1234");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrCreateTestUser("test-id", DeviceType.ANDROID);

        verify(userRepository).save(result);
        verify(userNotificationSettingRepository).save(any());
        assertThat(result).isNotSameAs(deletedUser);
        assertThat(result.getOauthId()).isEqualTo("test-id");
        assertThat(result.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(result.getNickname()).isEqualTo("재가입테스트1234");
        assertThat(result.isDeleted()).isFalse();
    }

    @Test
    void withdrawUserDeletesUserChildRecordsAndSoftDeletesUser() {
        User user = User.createTestUser("withdraw-test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(hikingRecordRepository.findRecordIdsOnlyParticipatedByUser(1L)).thenReturn(List.of(10L, 11L));

        userService.withdrawUser(user);

        verify(mountainLikeRepository).deleteByUser_Id(1L);
        verify(reviewRepository).deleteByUser_Id(1L);
        verify(courseDifficultyFeedbackRepository).deleteByUserId(1L);
        verify(hikingRecordRepository).findRecordIdsOnlyParticipatedByUser(1L);
        verify(hikingMemberRepository).deleteByUser_Id(1L);
        verify(courseDifficultyFeedbackRepository).deleteByHikingRecordIdIn(List.of(10L, 11L));
        verify(hikingRecordRepository).deleteAllByIdInBatch(List.of(10L, 11L));
        verify(notificationRepository).deleteAllByUserId(1L);
        verify(userOnboardingRepository).deleteByUser_Id(1L);
        verify(userNotificationSettingRepository).deleteByUser_Id(1L);
        verify(userRepository).save(user);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getName()).isNull();
        assertThat(user.getNickname()).isNull();
        assertThat(user.getOauthId()).isEqualTo("WITHDRAWN:1:TEST");
        assertThat(user.getOnboardingStatus()).isEqualTo(OnboardingStatus.INCOMPLETE);

        InOrder deleteOrder = inOrder(courseDifficultyFeedbackRepository, hikingRecordRepository);
        deleteOrder.verify(courseDifficultyFeedbackRepository).deleteByUserId(1L);
        deleteOrder.verify(hikingRecordRepository).findRecordIdsOnlyParticipatedByUser(1L);
        deleteOrder.verify(courseDifficultyFeedbackRepository).deleteByHikingRecordIdIn(List.of(10L, 11L));
        deleteOrder.verify(hikingRecordRepository).deleteAllByIdInBatch(List.of(10L, 11L));
    }

    @Test
    void withdrawUserSkipsHikingRecordBatchDeleteWhenNoOnlyParticipatedRecords() {
        User user = User.createTestUser("withdraw-test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(hikingRecordRepository.findRecordIdsOnlyParticipatedByUser(1L)).thenReturn(List.of());

        userService.withdrawUser(user);

        verify(courseDifficultyFeedbackRepository, never()).deleteByHikingRecordIdIn(any());
        verify(hikingRecordRepository, never()).deleteAllByIdInBatch(any());
        assertThat(user.isDeleted()).isTrue();
    }
}
