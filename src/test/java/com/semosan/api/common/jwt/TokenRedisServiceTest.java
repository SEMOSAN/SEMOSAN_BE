package com.semosan.api.common.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenRedisService tokenRedisService;

    @Test
    void saveRefreshTokenStoresHashedTokenWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenRedisService.saveRefreshToken(1L, "hashed", 1000L);

        verify(valueOperations).set("refresh:1", "hashed", 1000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void getRefreshTokenReadsByUserId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:1")).thenReturn("hashed");

        String token = tokenRedisService.getRefreshToken(1L);

        assertThat(token).isEqualTo("hashed");
    }

    @Test
    void deleteRefreshTokenDeletesByUserId() {
        tokenRedisService.deleteRefreshToken(1L);

        verify(redisTemplate).delete("refresh:1");
    }

    @Test
    void addToBlacklistStoresTokenWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenRedisService.addToBlacklist("access", 2000L);

        verify(valueOperations).set("blacklist:access", "true", 2000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void isBlacklistedReturnsWhetherBlacklistKeyExists() {
        when(redisTemplate.hasKey("blacklist:access")).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:missing")).thenReturn(false);

        assertThat(tokenRedisService.isBlacklisted("access")).isTrue();
        assertThat(tokenRedisService.isBlacklisted("missing")).isFalse();
    }
}
