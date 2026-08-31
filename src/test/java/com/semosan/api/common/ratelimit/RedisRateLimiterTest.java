package com.semosan.api.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RedisRateLimiter redisRateLimiter;

    @Test
    void allowsWhenCountIsWithinLimit() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any())).thenReturn(5L);

        RateLimitResult result = redisRateLimiter.tryConsume("global", "1.1.1.1", 10, 60);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void allowsWhenCountEqualsLimit() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any())).thenReturn(10L);

        RateLimitResult result = redisRateLimiter.tryConsume("global", "1.1.1.1", 10, 60);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void blocksWhenCountExceedsLimit() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any())).thenReturn(11L);

        RateLimitResult result = redisRateLimiter.tryConsume("global", "1.1.1.1", 10, 60);

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void passesRemainingWindowTimeAsScriptArgument() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any())).thenReturn(1L);
        ArgumentCaptor<String> ttlArg = ArgumentCaptor.forClass(String.class);

        redisRateLimiter.tryConsume("auth", "1.1.1.1", 10, 60);

        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), ttlArg.capture());
        // 윈도우 전체 길이(60)가 아니라 "윈도우 끝까지 남은 시간"이 전달돼야 한다 — 항상 1~60 범위.
        assertThat(Long.parseLong(ttlArg.getValue())).isBetween(1L, 60L);
    }

    @Test
    void failsOpenWhenRedisThrows() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenThrow(new RuntimeException("redis down"));

        RateLimitResult result = redisRateLimiter.tryConsume("global", "1.1.1.1", 10, 60);

        assertThat(result.allowed()).isTrue();
    }
}
