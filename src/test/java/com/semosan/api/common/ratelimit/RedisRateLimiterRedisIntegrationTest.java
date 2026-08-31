package com.semosan.api.common.ratelimit;

import com.semosan.api.common.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisRateLimiter가 실제 Redis에서 rate-limit-incr.lua를 통해 동작하는지 검증한다.
 * RedisRateLimiterTest는 StringRedisTemplate.execute()를 mock해 Lua 스크립트 자체는 실행하지 않으므로,
 * 스크립트 로딩/문법/atomic TTL 부여를 이 테스트가 실제 라운드트립으로 커버한다.
 * 로컬 Redis가 없으면 이 테스트만 인프라 부재로 실패한다 — 코드 문제와 별개로 봐야 한다.
 */
@SpringBootTest(classes = RedisRateLimiterRedisIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class RedisRateLimiterRedisIntegrationTest {

    private static final String KEY_PREFIX = "ratelimit:integration-test:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @AfterEach
    void cleanUp() {
        redisTemplate.keys(KEY_PREFIX + "*").forEach(redisTemplate::delete);
    }

    @Test
    void firstRequestGetsPositiveTtlAndIsAllowed() {
        String clientId = "client-" + System.nanoTime();

        RateLimitResult result = redisRateLimiter.tryConsume("integration-test", clientId, 3, 60);

        assertThat(result.allowed()).isTrue();
        Long ttl = redisTemplate.getExpire(onlyKey(clientId));
        assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(60);
    }

    @Test
    void blocksOnceLimitExceededWithinSameWindow() {
        String clientId = "client-" + System.nanoTime();

        redisRateLimiter.tryConsume("integration-test", clientId, 2, 60);
        redisRateLimiter.tryConsume("integration-test", clientId, 2, 60);
        RateLimitResult third = redisRateLimiter.tryConsume("integration-test", clientId, 2, 60);

        assertThat(third.allowed()).isFalse();
    }

    @Test
    void healsKeyThatWasLeftWithoutTtlByOldBuggyBehavior() {
        String clientId = "client-" + System.nanoTime();
        String key = onlyKey(clientId);
        // 옛 비원자 로직이 EXPIRE 유실로 남겨놓을 수 있던 상태를 재현: count는 쌓여 있는데 TTL이 없음.
        redisTemplate.opsForValue().set(key, "5");
        assertThat(redisTemplate.getExpire(key)).isEqualTo(-1L);

        redisRateLimiter.tryConsume("integration-test", clientId, 10, 60);

        Long ttl = redisTemplate.getExpire(key);
        assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(60);
    }

    private static String onlyKey(String clientId) {
        long windowIndex = (System.currentTimeMillis() / 1000) / 60;
        return KEY_PREFIX + clientId + ":" + windowIndex;
    }

    @EnableAutoConfiguration
    @Import({RedisConfig.class, RedisRateLimiter.class})
    static class TestConfig {
    }
}
