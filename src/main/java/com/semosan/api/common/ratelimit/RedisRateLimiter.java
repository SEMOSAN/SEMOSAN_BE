package com.semosan.api.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 윈도우 내 요청 수를 1 증가시키고 한도 초과 여부를 반환한다.
     *
     * @param scope         정책 구분
     * @param clientId      식별자
     * @param limit         윈도우당 허용 요청 수
     * @param windowSeconds 윈도우 길이(초)
     */
    public RateLimitResult tryConsume(String scope, String clientId, int limit, long windowSeconds) {
        long nowSeconds = System.currentTimeMillis() / 1000;
        long windowIndex = nowSeconds / windowSeconds;
        long retryAfter = windowSeconds - (nowSeconds % windowSeconds);
        String key = KEY_PREFIX + scope + ":" + clientId + ":" + windowIndex;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            // 윈도우 첫 요청일 때만 TTL 부여 (이후 요청은 기존 TTL 유지)
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            boolean allowed = count == null || count <= limit;
            return new RateLimitResult(allowed, retryAfter);
        } catch (RuntimeException e) {
            log.warn("레이트리밋 검사 실패, fail-open 처리 (scope={}, clientId={}): {}",
                    scope, clientId, e.getMessage());
            return new RateLimitResult(true, retryAfter);
        }
    }
}
