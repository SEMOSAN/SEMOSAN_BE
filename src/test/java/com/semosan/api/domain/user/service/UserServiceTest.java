package com.semosan.api.domain.user.service;

import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.notification.repository.NotificationRepository;
import com.semosan.api.domain.review.repository.ReviewRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import com.semosan.api.domain.user.policy.NicknamePolicy;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private ReviewRepository reviewRepository;

    @Mock
    private HikingMemberRepository hikingMemberRepository;

    @Mock
    private HikingRecordRepository hikingRecordRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NicknamePolicy nicknamePolicy;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private UserService userService;

    @Test
    void findOrRegisterKakaoUserReturnsActiveUserWhenExists() {
        User user = User.createKakaoUser("kakao-id", "user@example.com", "name", "profile", DeviceType.IOS);
        when(userRepository.findByOauthIdAndOauthProvider("kakao-id", OAuthProvider.KAKAO))
                .thenReturn(Optional.of(user));

        User result = userService.findOrRegisterKakaoUser(
                "kakao-id",
                "new@example.com",
                "new-name",
                "new-profile",
                DeviceType.ANDROID
        );

        assertThat(result).isSameAs(user);
    }

    @Test
    void findOrRegisterKakaoUserCreatesNewUserWhenActiveUserDoesNotExist() {
        when(userRepository.findByOauthIdAndOauthProvider("kakao-id", OAuthProvider.KAKAO))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrRegisterKakaoUser(
                "kakao-id",
                "user@example.com",
                "name",
                "profile",
                DeviceType.IOS
        );

        verify(userRepository).save(result);
        verify(userNotificationSettingRepository).save(any());
        assertThat(result.getOauthId()).isEqualTo("kakao-id");
        assertThat(result.getOauthProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.isDeleted()).isFalse();
    }

    @Test
    void findOrRegisterKakaoUserCreatesNewUserWhenMatchedUserIsDeleted() {
        User deletedUser = User.createKakaoUser("kakao-id", "old@example.com", "old-name", "old-profile", DeviceType.IOS);
        ReflectionTestUtils.setField(deletedUser, "id", 1L);
        deletedUser.withdraw();
        ReflectionTestUtils.setField(deletedUser, "oauthId", "kakao-id");

        when(userRepository.findByOauthIdAndOauthProvider("kakao-id", OAuthProvider.KAKAO))
                .thenReturn(Optional.of(deletedUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrRegisterKakaoUser(
                "kakao-id",
                "user@example.com",
                "name",
                "profile",
                DeviceType.IOS
        );

        verify(userRepository).save(result);
        verify(userNotificationSettingRepository).save(any());
        assertThat(result).isNotSameAs(deletedUser);
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
        verify(hikingRecordRepository).findRecordIdsOnlyParticipatedByUser(1L);
        verify(hikingMemberRepository).deleteByUser_Id(1L);
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
    }
}
