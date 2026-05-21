package com.semosan.api.domain.user.service;

import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.notification.repository.NotificationRepository;
import com.semosan.api.domain.review.repository.ReviewRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
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

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(user.getOnboardingStatus()).isEqualTo(OnboardingStatus.INCOMPLETE);
    }
}
