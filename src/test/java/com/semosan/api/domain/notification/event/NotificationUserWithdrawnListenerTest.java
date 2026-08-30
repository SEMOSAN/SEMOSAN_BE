package com.semosan.api.domain.notification.event;

import com.semosan.api.domain.notification.repository.NotificationRepository;
import com.semosan.api.domain.user.event.UserWithdrawnEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationUserWithdrawnListenerTest {

    @Test
    void onUserWithdrawnDeletesNotifications() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationUserWithdrawnListener listener = new NotificationUserWithdrawnListener(notificationRepository);

        listener.onUserWithdrawn(new UserWithdrawnEvent(1L));

        verify(notificationRepository).deleteAllByUserId(1L);
    }
}
