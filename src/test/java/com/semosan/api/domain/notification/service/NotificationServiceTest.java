package com.semosan.api.domain.notification.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.notification.dto.response.NotificationResponse;
import com.semosan.api.domain.notification.entity.FcmToken;
import com.semosan.api.domain.notification.entity.Notification;
import com.semosan.api.domain.notification.enums.NotificationTargetType;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.event.NotificationCreatedEvent;
import com.semosan.api.domain.notification.repository.FcmTokenRepository;
import com.semosan.api.domain.notification.repository.NotificationRepository;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendSavesNotificationAndPublishesEventWhenTokensExist() {
        Notification notification = Notification.create(
                1L,
                NotificationType.COMMUNITY_COMMENT,
                "title",
                "body",
                Map.of()
        );
        ReflectionTestUtils.setField(notification, "id", 10L);
        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(fcmTokenRepository.findAllByUserId(1L)).thenReturn(List.of(FcmToken.create(1L, "token", DeviceType.IOS)));
        ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);

        notificationService.send(
                1L,
                NotificationType.COMMUNITY_COMMENT,
                Map.of("actorName", "푸름", "commentPreview", "확인")
        );

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().command().notificationId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().command().tokens()).containsExactly("token");
        assertThat(eventCaptor.getValue().command().title()).isEqualTo("새 댓글이 달렸어요");
        assertThat(eventCaptor.getValue().command().body()).isEqualTo("푸름: 확인");
    }

    @Test
    void sendUsesBodyOverrideWhenProvided() {
        Notification notification = Notification.create(
                1L,
                NotificationType.TRACKING_PHOTO_MILESTONE,
                "SEMOSAN",
                "override",
                Map.of("distance", 500, "milestoneIndex", 3)
        );
        ReflectionTestUtils.setField(notification, "id", 10L);
        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(fcmTokenRepository.findAllByUserId(1L)).thenReturn(List.of(FcmToken.create(1L, "token", DeviceType.IOS)));
        ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);

        notificationService.send(
                1L,
                NotificationType.TRACKING_PHOTO_MILESTONE,
                Map.of("distance", 500, "milestoneIndex", 3),
                "override"
        );

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().command().body()).isEqualTo("override");
    }

    @Test
    void sendReturnsWithoutEventWhenUserHasNoTokens() {
        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fcmTokenRepository.findAllByUserId(1L)).thenReturn(List.of());

        notificationService.send(
                1L,
                NotificationType.COMMUNITY_COMMENT,
                Map.of("actorName", "푸름", "commentPreview", "확인")
        );

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void sendThrowsWhenReceiverDoesNotExist() {
        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.send(
                1L,
                NotificationType.COMMUNITY_COMMENT,
                Map.of("actorName", "푸름", "commentPreview", "확인")
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.USER_NOT_FOUND);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendThrowsWhenRequiredParamsAreMissing() {
        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);

        assertThatThrownBy(() -> notificationService.send(
                1L,
                NotificationType.COMMUNITY_COMMENT,
                Map.of("actorName", "푸름")
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_PARAMS_INVALID);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getNotificationsMapsEntitiesToResponsesWithDestination() {
        Notification notification = Notification.create(
                1L,
                NotificationType.SEMOFEED_EMOJI,
                "세모피드에 반응이 달렸어요",
                "푸름님이 세모피드에 🔥 반응을 남겼어요",
                Map.of("actorId", 2L, "actorName", "푸름", "semoFeedId", 42L, "emojiType", "🔥")
        );
        ReflectionTestUtils.setField(notification, "id", 10L);
        PageRequest pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));

        Page<NotificationResponse> result = notificationService.getNotifications(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        NotificationResponse response = result.getContent().getFirst();
        assertThat(response.notificationId()).isEqualTo(10L);
        assertThat(response.targetType()).isEqualTo(NotificationTargetType.SEMOFEED);
        assertThat(response.targetId()).isEqualTo(42L);
    }

    @Test
    void getNotificationsReturnsEmptyPageWhenUserHasNoNotifications() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(notificationService.getNotifications(1L, pageable)).isEmpty();
    }

    @Test
    void getUnreadCountDelegatesToRepository() {
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(3L);

        assertThat(notificationService.getUnreadCount(1L)).isEqualTo(3L);
    }

    @Test
    void markAsReadFlipsReadStateOfOwnedNotification() {
        Notification notification = Notification.create(
                1L,
                NotificationType.COMMUNITY_COMMENT,
                "title",
                "body",
                Map.of("postId", 7L)
        );
        when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 10L);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void markAsReadThrowsWhenNotificationBelongsToAnotherUser() {
        // 소유자 조건까지 걸린 조회라 남의 알림은 애초에 조회되지 않는다.
        when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void markAllAsReadDelegatesToBulkUpdate() {
        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsReadByUserId(eq(1L), any(LocalDateTime.class));
    }
}
