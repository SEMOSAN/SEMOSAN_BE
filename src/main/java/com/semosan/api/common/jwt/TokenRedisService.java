package com.semosan.api.common.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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
