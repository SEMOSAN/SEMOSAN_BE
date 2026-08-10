package com.semosan.api.domain.auth.event;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.domain.auth.dispatcher.UserWithdrawCleanupDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawCleanupEventListener {

    private final JwtService jwtService;
    private final UserWithdrawCleanupDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserWithdrawCleanupRequested(UserWithdrawCleanupRequestedEvent event) {
        invalidateTokens(event);
        dispatcher.dispatch(event);
    }

    // logout()과 동일하게 액세스 토큰 블랙리스트 + refresh token 삭제는 동기로 즉시 처리한다.
    // JwtFilter가 탈퇴(deleted) 유저를 별도로 걸러내므로, 여기서 실패해도 보안 구멍으로 이어지지 않는다.
    private void invalidateTokens(UserWithdrawCleanupRequestedEvent event) {
        try {
            jwtService.blacklistAccessToken(event.accessToken());
            jwtService.deleteRefreshToken(event.userId());
        } catch (RuntimeException e) {
            log.warn("회원 탈퇴 후 토큰 무효화 실패 (userId={}): {}", event.userId(), e.getMessage());
        }
    }
}
