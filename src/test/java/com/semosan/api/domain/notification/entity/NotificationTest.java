package com.semosan.api.domain.notification.entity;

import com.semosan.api.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    void createInitializesUnreadNotification() {
        Map<String, Object> extras = Map.of("actorName", "푸름", "commentPreview", "댓글");

        Notification notification = Notification.create(
                1L,
                NotificationType.COMMUNITY_COMMENT,
                "새 댓글이 달렸어요",
                "푸름: 댓글",
                extras
        );

        assertThat(notification.getUserId()).isEqualTo(1L);
        assertThat(notification.getType()).isEqualTo(NotificationType.COMMUNITY_COMMENT);
        assertThat(notification.getTitle()).isEqualTo("새 댓글이 달렸어요");
        assertThat(notification.getBody()).isEqualTo("푸름: 댓글");
        assertThat(notification.getExtras()).isSameAs(extras);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();
    }

    @Test
    void markAsReadMarksUnreadNotificationAndSetsReadAt() {
        Notification notification = Notification.create(
                1L,
                NotificationType.COMMUNITY_POST_LIKE,
                "새 좋아요",
                "푸름님이 좋아요를 눌렀어요",
                Map.of("actorId", 2L, "postId", 10L)
        );

        notification.markAsRead();

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void markAsReadDoesNotChangeReadAtWhenAlreadyRead() {
        Notification notification = Notification.create(
                1L,
                NotificationType.COMMUNITY_POST_LIKE,
                "새 좋아요",
                "푸름님이 좋아요를 눌렀어요",
                Map.of("actorId", 2L, "postId", 10L)
        );
        notification.markAsRead();
        LocalDateTime firstReadAt = notification.getReadAt();

        notification.markAsRead();

        assertThat(notification.getReadAt()).isSameAs(firstReadAt);
    }
}
