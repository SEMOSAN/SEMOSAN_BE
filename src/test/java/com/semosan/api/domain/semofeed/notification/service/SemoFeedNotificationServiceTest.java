package com.semosan.api.domain.semofeed.notification.service;

import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemoFeedNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SemoFeedNotificationService semoFeedNotificationService;

    @Test
    void semoFeedEmojiNotificationTypeRequiresNavigationPayload() {
        assertThatCode(() -> NotificationType.SEMOFEED_EMOJI.validate(Map.of(
                "actorId", 2L,
                "actorName", "reactor",
                "semoFeedId", 10L,
                "emojiType", "FIRE"
        ))).doesNotThrowAnyException();
    }

    @Test
    void sendEmojiNotificationSendsToSemoFeedAuthor() {
        User author = user(1L, "author");
        User reactor = user(2L, "reactor");
        SemoFeed semoFeed = semoFeed(10L, author);

        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);

        semoFeedNotificationService.sendEmojiNotification(semoFeed, reactor, SemoFeedEmojiType.FIRE);

        verify(notificationService).send(
                eq(1L),
                eq(NotificationType.SEMOFEED_EMOJI),
                argThat(params -> params.get("actorId").equals(2L)
                        && params.get("actorName").equals("reactor")
                        && params.get("semoFeedId").equals(10L)
                        && params.get("emojiType").equals("🔥"))
        );
    }

    @Test
    void sendEmojiNotificationSkipsSelfReaction() {
        User author = user(1L, "author");
        SemoFeed semoFeed = semoFeed(10L, author);

        semoFeedNotificationService.sendEmojiNotification(semoFeed, author, SemoFeedEmojiType.HEART);

        verify(notificationService, never()).send(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void sendEmojiNotificationSkipsInactiveAuthor() {
        User author = user(1L, "author");
        User reactor = user(2L, "reactor");
        SemoFeed semoFeed = semoFeed(10L, author);

        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(false);

        semoFeedNotificationService.sendEmojiNotification(semoFeed, reactor, SemoFeedEmojiType.LAUGH);

        verify(notificationService, never()).send(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    private SemoFeed semoFeed(Long id, User user) {
        SemoFeed semoFeed = SemoFeed.create(user, "https://example.com/semofeed.png");
        ReflectionTestUtils.setField(semoFeed, "id", id);
        return semoFeed;
    }
}
