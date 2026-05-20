package com.semosan.api.domain.auth.dispatcher;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.domain.auth.event.UserWithdrawCleanupRequestedEvent;
import com.semosan.api.domain.notification.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawCleanupDispatcher {

    private static final int MAX_RETRY_COUNT = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    private final JwtService jwtService;
    private final FcmTokenService fcmTokenService;

    @Async("authCleanupTaskExecutor")
    public void dispatch(UserWithdrawCleanupRequestedEvent event) {
        cleanupWithRetry(event);
    }

    void cleanupWithRetry(UserWithdrawCleanupRequestedEvent event) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                fcmTokenService.deleteAllByUserId(event.userId());
                jwtService.blacklistAccessToken(event.accessToken());
                jwtService.deleteRefreshToken(event.userId());
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn(
                        "회원 탈퇴 후 JWT 정리 실패 (attempt={}/{}, userId={}): {}",
                        attempt,
                        MAX_RETRY_COUNT,
                        event.userId(),
                        e.getMessage()
                );

                if (attempt < MAX_RETRY_COUNT) {
                    if (!sleepBackoff(attempt)) {
                        log.warn("회원 탈퇴 후 JWT/FCM 정리 재시도 중단 (userId={})", event.userId());
                        return;
                    }
                }
            }
        }

        log.error("회원 탈퇴 후 JWT/FCM 정리 최종 실패 (userId={})", event.userId(), lastFailure);
    }

    boolean sleepBackoff(int attempt) {
        long delayMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(delayMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
