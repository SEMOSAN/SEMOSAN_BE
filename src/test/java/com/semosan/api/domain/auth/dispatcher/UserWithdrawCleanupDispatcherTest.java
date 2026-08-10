package com.semosan.api.domain.auth.dispatcher;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.domain.auth.event.UserWithdrawCleanupRequestedEvent;
import com.semosan.api.domain.notification.service.FcmTokenService;
import com.semosan.api.domain.oauth.client.OAuthKakaoClient;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UserWithdrawCleanupDispatcherTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FcmTokenService fcmTokenService;

    @Mock
    private OAuthKakaoClient oAuthKakaoClient;

    @InjectMocks
    private UserWithdrawCleanupDispatcher dispatcher;

    @Test
    void cleanupWithRetry_unlinksKakaoWhenProviderIsKakao() {
        UserWithdrawCleanupRequestedEvent event =
                new UserWithdrawCleanupRequestedEvent(1L, "access-token", "kakao-1", OAuthProvider.KAKAO);

        dispatcher.cleanupWithRetry(event);

        verify(oAuthKakaoClient).unlinkKakaoUser("kakao-1");
    }

    @Test
    void cleanupWithRetry_skipsKakaoUnlinkWhenProviderIsNotKakao() {
        UserWithdrawCleanupRequestedEvent event =
                new UserWithdrawCleanupRequestedEvent(1L, "access-token", "apple-1", OAuthProvider.APPLE);

        dispatcher.cleanupWithRetry(event);

        verify(oAuthKakaoClient, never()).unlinkKakaoUser(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void cleanupWithRetry_retriesOnceWhenJwtFails() {
        UserWithdrawCleanupRequestedEvent event = new UserWithdrawCleanupRequestedEvent(1L, "access-token", "kakao-1", OAuthProvider.KAKAO);

        doNothing().when(fcmTokenService).deleteAllByUserId(1L);
        doThrow(new RuntimeException("redis down"))
                .doNothing()
                .when(jwtService).blacklistAccessToken("access-token");
        doNothing().when(jwtService).deleteRefreshToken(1L);

        assertDoesNotThrow(() -> dispatcher.cleanupWithRetry(event));

        verify(jwtService, times(2)).blacklistAccessToken("access-token");
        verify(jwtService, times(1)).deleteRefreshToken(1L);
        verify(fcmTokenService, times(2)).deleteAllByUserId(1L);
    }

    @Test
    void dispatchDelegatesToCleanup() {
        UserWithdrawCleanupRequestedEvent event = new UserWithdrawCleanupRequestedEvent(1L, "access-token", "kakao-1", OAuthProvider.KAKAO);
        doNothing().when(fcmTokenService).deleteAllByUserId(1L);
        doNothing().when(jwtService).blacklistAccessToken("access-token");
        doNothing().when(jwtService).deleteRefreshToken(1L);

        dispatcher.dispatch(event);

        verify(fcmTokenService).deleteAllByUserId(1L);
        verify(jwtService).blacklistAccessToken("access-token");
        verify(jwtService).deleteRefreshToken(1L);
    }

    @Test
    void cleanupWithRetryStopsAfterThreeFailures() {
        UserWithdrawCleanupRequestedEvent event = new UserWithdrawCleanupRequestedEvent(1L, "access-token", "kakao-1", OAuthProvider.KAKAO);
        doThrow(new RuntimeException("fcm down")).when(fcmTokenService).deleteAllByUserId(1L);

        assertDoesNotThrow(() -> dispatcher.cleanupWithRetry(event));

        verify(fcmTokenService, times(3)).deleteAllByUserId(1L);
        verify(jwtService, never()).blacklistAccessToken("access-token");
        verify(jwtService, never()).deleteRefreshToken(1L);
    }

    @Test
    void cleanupWithRetryStopsWhenBackoffIsInterrupted() {
        UserWithdrawCleanupRequestedEvent event = new UserWithdrawCleanupRequestedEvent(1L, "access-token", "kakao-1", OAuthProvider.KAKAO);
        doThrow(new RuntimeException("fcm down")).when(fcmTokenService).deleteAllByUserId(1L);
        Thread.currentThread().interrupt();

        try {
            assertDoesNotThrow(() -> dispatcher.cleanupWithRetry(event));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        verify(fcmTokenService, times(1)).deleteAllByUserId(1L);
        verify(jwtService, never()).blacklistAccessToken("access-token");
        verify(jwtService, never()).deleteRefreshToken(1L);
    }

    @Test
    void sleepBackoff_returnsTrueWhenSleepCompletes() {
        assertTrue(dispatcher.sleepBackoff(1));
    }

    @Test
    void sleepBackoff_returnsFalseWhenInterrupted() {
        Thread.currentThread().interrupt();

        try {
            assertFalse(dispatcher.sleepBackoff(1));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
