package com.semosan.api.domain.auth.event;

import com.semosan.api.domain.user.enums.user.OAuthProvider;

// oauthId/provider는 User.withdraw()로 익명화되기 전의 원본 값을 담는다.
// (카카오 연동 해제 등 탈퇴 후 정리 작업에 필요)
public record UserWithdrawCleanupRequestedEvent(
        Long userId,
        String accessToken,
        String oauthId,
        OAuthProvider provider
) {
}
