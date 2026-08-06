package com.semosan.api.domain.auth.event;

import com.semosan.api.domain.auth.dispatcher.UserWithdrawCleanupDispatcher;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserWithdrawCleanupEventListenerTest {

    @Test
    void onUserWithdrawCleanupRequestedDelegatesToDispatcher() {
        UserWithdrawCleanupDispatcher dispatcher = mock(UserWithdrawCleanupDispatcher.class);
        UserWithdrawCleanupEventListener listener = new UserWithdrawCleanupEventListener(dispatcher);
        UserWithdrawCleanupRequestedEvent event = new UserWithdrawCleanupRequestedEvent(1L, "access-token");

        listener.onUserWithdrawCleanupRequested(event);

        verify(dispatcher).dispatch(event);
    }
}
