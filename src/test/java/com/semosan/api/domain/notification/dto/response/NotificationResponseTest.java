package com.semosan.api.domain.notification.dto.response;

import com.semosan.api.domain.notification.entity.Notification;
import com.semosan.api.domain.notification.enums.NotificationTargetType;
import com.semosan.api.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationResponseTest {

    @Test
    void fromMapsSemoFeedEmojiNotificationToFeedDestination() {
        Notification notification = notification(
                NotificationType.SEMOFEED_EMOJI,
                "세모피드에 반응이 달렸어요",
                "푸름님이 세모피드에 🔥 반응을 남겼어요",
                Map.of("actorId", 1L, "actorName", "푸름", "semoFeedId", 42L, "emojiType", "🔥")
        );

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.notificationId()).isEqualTo(10L);
        assertThat(response.type()).isEqualTo(NotificationType.SEMOFEED_EMOJI);
        assertThat(response.title()).isEqualTo("세모피드에 반응이 달렸어요");
        assertThat(response.body()).isEqualTo("푸름님이 세모피드에 🔥 반응을 남겼어요");
        assertThat(response.targetType()).isEqualTo(NotificationTargetType.SEMOFEED);
        assertThat(response.targetId()).isEqualTo(42L);
        assertThat(response.isRead()).isFalse();
    }

    @Test
    void fromMapsCommunityNotificationToPostDestination() {
        Notification notification = notification(
                NotificationType.COMMUNITY_COMMENT,
                "새 댓글이 달렸어요",
                "푸름: 확인",
                Map.of("actorName", "푸름", "commentPreview", "확인", "postId", 7L)
        );

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.targetType()).isEqualTo(NotificationTargetType.COMMUNITY_POST);
        assertThat(response.targetId()).isEqualTo(7L);
    }

    @Test
    void fromReturnsNoneWhenNotificationHasNoDestination() {
        Notification notification = notification(
                NotificationType.TRACKING_PHOTO_MILESTONE,
                "SEMOSAN",
                "500m 돌파! 인증 사진을 남겨보세요!",
                Map.of("distance", 500, "milestoneIndex", 3)
        );

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.targetType()).isEqualTo(NotificationTargetType.NONE);
        assertThat(response.targetId()).isNull();
    }

    @Test
    void fromDowngradesToNoneWhenTargetIdIsMissing() {
        // extras 에 semoFeedId 가 없으면 앱이 빈 세모피드 화면으로 이동하게 되므로 이동 불가로 내려야 한다.
        Notification notification = notification(
                NotificationType.SEMOFEED_EMOJI,
                "세모피드에 반응이 달렸어요",
                "푸름님이 세모피드에 🔥 반응을 남겼어요",
                Map.of("actorName", "푸름")
        );

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.targetType()).isEqualTo(NotificationTargetType.NONE);
        assertThat(response.targetId()).isNull();
    }

    @Test
    void fromCarriesReadStateAndCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 5, 14, 20);
        Notification notification = notification(
                NotificationType.COMMUNITY_POST_LIKE,
                "게시글에 좋아요가 눌렸어요",
                "푸름님이 게시글을 좋아합니다",
                Map.of("actorName", "푸름", "postId", 7L)
        );
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        notification.markAsRead();

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.isRead()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    private Notification notification(
            NotificationType type,
            String title,
            String body,
            Map<String, Object> extras
    ) {
        Notification notification = Notification.create(1L, type, title, body, extras);
        ReflectionTestUtils.setField(notification, "id", 10L);
        return notification;
    }
}
