package com.semosan.api.common.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 유저당 세션 1개만 유지한다. refresh 토큰을 {@code refresh:{userId}} 키 하나에 저장해
 * 새 로그인이 이전 기기의 토큰을 덮어쓰는 구조다.
 *
 * 트래킹이 유저당 활성 세션 1개를 강제하고 있어(uq_tracking_sessions_user_active)
 * 멀티 기기와는 개념이 맞지 않는다. 기기별 세션으로 바꾸려면 로그인·로그아웃 시점의
 * FCM 토큰 정리(AuthService, OAuthLoginProcessor)도 기기 단위로 함께 손봐야 한다.
 */
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(Long userId, String hashedToken, long expirationMs) {
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + userId,
                hashedToken,
                expirationMs,
                TimeUnit.MILLISECONDS
        );
    }

    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    public void addToBlacklist(String accessToken, long remainingMs) {
        redisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + accessToken,
                "true",
                remainingMs,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + accessToken));
    }
}
