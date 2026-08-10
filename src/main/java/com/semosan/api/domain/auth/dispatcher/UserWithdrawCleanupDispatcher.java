package com.semosan.api.domain.auth.dispatcher;

import com.semosan.api.domain.auth.event.UserWithdrawCleanupRequestedEvent;
import com.semosan.api.domain.notification.service.FcmTokenService;
import com.semosan.api.domain.oauth.client.OAuthKakaoClient;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// FCM 토큰 삭제, 카카오 연동 해제 등 외부 서비스 정리 작업을 담당한다.
// JWT 블랙리스트 처리는 UserWithdrawCleanupEventListener에서 동기로 즉시 처리하므로 여기서 다루지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawCleanupDispatcher {

    private static final int MAX_RETRY_COUNT = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    private final FcmTokenService fcmTokenService;
    private final OAuthKakaoClient oAuthKakaoClient;

    @Async("authCleanupTaskExecutor")
    public void dispatch(UserWithdrawCleanupRequestedEvent event) {
        cleanupWithRetry(event);
    }

    void cleanupWithRetry(UserWithdrawCleanupRequestedEvent event) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                fcmTokenService.deleteAllByUserId(event.userId());
                if (event.provider() == OAuthProvider.KAKAO) {
                    oAuthKakaoClient.unlinkKakaoUser(event.oauthId());
                }
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn(
                        "회원 탈퇴 후 FCM/카카오 정리 실패 (attempt={}/{}, userId={}): {}",
                        attempt,
                        MAX_RETRY_COUNT,
                        event.userId(),
                        e.getMessage()
                );

                if (attempt < MAX_RETRY_COUNT) {
                    if (!sleepBackoff(attempt)) {
                        log.warn("회원 탈퇴 후 FCM/카카오 정리 재시도 중단 (userId={})", event.userId());
                        return;
                    }
                }
            }
        }

        log.error("회원 탈퇴 후 FCM/카카오 정리 최종 실패 (userId={})", event.userId(), lastFailure);
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
