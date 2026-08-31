package com.semosan.api.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private static final DefaultRedisScript<Long> INCR_SCRIPT;

    static {
        INCR_SCRIPT = new DefaultRedisScript<>();
        INCR_SCRIPT.setLocation(new ClassPathResource("redis/rate-limit-incr.lua"));
        INCR_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    /**
     * 윈도우 내 요청 수를 1 증가시키고 한도 초과 여부를 반환한다.
     * INCR + EXPIRE 를 Lua 스크립트로 원자 처리하고, 매 호출마다 "윈도우 끝까지 남은 시간"으로 EXPIRE 를
     * 다시 걸어 절대 만료 시각을 윈도우 끝 지점에 고정한다 — 과거 배포에서 TTL 없이 남은 키도 다음 요청에서 자동 복구된다.
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
            Long count = redisTemplate.execute(INCR_SCRIPT, List.of(key), String.valueOf(retryAfter));
            boolean allowed = count == null || count <= limit;
            return new RateLimitResult(allowed, retryAfter);
        } catch (RuntimeException e) {
            log.warn("레이트리밋 검사 실패, fail-open 처리 (scope={}, clientId={}): {}",
                    scope, clientId, e.getMessage());
            return new RateLimitResult(true, retryAfter);
        }
    }
}
