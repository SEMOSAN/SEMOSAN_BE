package com.semosan.api.common.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * RedisConfig.errorHandler()가 캐시 오류를 삼키고(fail-open) 예외를 전파하지 않는지 확인한다.
 * Redis 장애 시 이 핸들러가 없으면 @Cacheable/@CacheEvict가 예외를 그대로 던져
 * 댓글 목록/코스 상세 조회 등 원본 API까지 실패한다 (PR #358 리뷰 지적).
 */
class RedisConfigTest {

    private final CacheErrorHandler errorHandler = new RedisConfig().errorHandler();
    private final Cache cache = Mockito.mock(Cache.class);
    private final RuntimeException redisDown = new RuntimeException("Redis connection refused");

    @Test
    void handleCacheGetErrorDoesNotPropagate() {
        assertThatCode(() -> errorHandler.handleCacheGetError(redisDown, cache, "key"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleCachePutErrorDoesNotPropagate() {
        assertThatCode(() -> errorHandler.handleCachePutError(redisDown, cache, "key", "value"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleCacheEvictErrorDoesNotPropagate() {
        assertThatCode(() -> errorHandler.handleCacheEvictError(redisDown, cache, "key"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleCacheClearErrorDoesNotPropagate() {
        assertThatCode(() -> errorHandler.handleCacheClearError(redisDown, cache))
                .doesNotThrowAnyException();
    }
}
