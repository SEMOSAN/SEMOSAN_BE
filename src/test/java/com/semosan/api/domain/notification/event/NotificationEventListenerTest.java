package com.semosan.api.domain.notification.event;

import com.semosan.api.domain.notification.dispatcher.NotificationDispatchCommand;
import com.semosan.api.domain.notification.dispatcher.NotificationDispatcher;
import com.semosan.api.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationEventListenerTest {

    @Test
    void onNotificationCreatedDispatchesCommand() {
        NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
        NotificationEventListener listener = new NotificationEventListener(dispatcher);
        NotificationDispatchCommand command = new NotificationDispatchCommand(
                1L,
                2L,
                NotificationType.COMMUNITY_COMMENT,
                "title",
                "body",
                Map.of("actorName", "푸름", "commentPreview", "확인"),
                List.of("token")
        );

        listener.onNotificationCreated(new NotificationCreatedEvent(command));

        verify(dispatcher).dispatch(command);
    }
}
