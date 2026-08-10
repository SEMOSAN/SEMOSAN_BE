package com.semosan.api.domain.auth.event;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.domain.auth.dispatcher.UserWithdrawCleanupDispatcher;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserWithdrawCleanupEventListenerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserWithdrawCleanupDispatcher dispatcher;

    @Test
    void onUserWithdrawCleanupRequestedInvalidatesTokensAndDelegatesToDispatcher() {
        UserWithdrawCleanupEventListener listener = new UserWithdrawCleanupEventListener(jwtService, dispatcher);
        UserWithdrawCleanupRequestedEvent event =
                new UserWithdrawCleanupRequestedEvent(1L, "access-token", "kakao-1", OAuthProvider.KAKAO);

        listener.onUserWithdrawCleanupRequested(event);

        verify(jwtService).blacklistAccessToken("access-token");
        verify(jwtService).deleteRefreshToken(1L);
        verify(dispatcher).dispatch(event);
    }

    @Test
    void onUserWithdrawCleanupRequestedStillDelegatesWhenTokenInvalidationFails() {
        UserWithdrawCleanupEventListener listener = new UserWithdrawCleanupEventListener(jwtService, dispatcher);
        UserWithdrawCleanupRequestedEvent event =
                new UserWithdrawCleanupRequestedEvent(1L, "access-token", "kakao-1", OAuthProvider.KAKAO);
        doThrow(new RuntimeException("redis down")).when(jwtService).blacklistAccessToken("access-token");

        assertDoesNotThrow(() -> listener.onUserWithdrawCleanupRequested(event));

        verify(dispatcher).dispatch(event);
    }
}
