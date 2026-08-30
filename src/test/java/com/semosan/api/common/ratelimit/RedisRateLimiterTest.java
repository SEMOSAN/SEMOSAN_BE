package com.semosan.api.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisRateLimiter redisRateLimiter;

    @Test
    void allowsWhenCountIsWithinLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        RateLimitResult result = redisRateLimiter.tryConsume("global", "1.1.1.1", 10, 60);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void allowsWhenCountEqualsLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(10L);

        RateLimitResult result = redisRateLimiter.tryConsume("global", "1.1.1.1", 10, 60);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void blocksWhenCountExceedsLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(11L);

        RateLimitResult result = redisRateLimiter.tryConsume("global", "1.1.1.1", 10, 60);

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void setsTtlOnFirstRequestInWindow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        redisRateLimiter.tryConsume("auth", "1.1.1.1", 10, 60);

        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void doesNotSetTtlOnSubsequentRequests() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(2L);

        redisRateLimiter.tryConsume("auth", "1.1.1.1", 10, 60);

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void failsOpenWhenRedisThrows() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        RateLimitResult result = redisRateLimiter.tryConsume("global", "1.1.1.1", 10, 60);

        assertThat(result.allowed()).isTrue();
    }
}
