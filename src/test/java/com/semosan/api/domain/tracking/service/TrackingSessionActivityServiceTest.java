package com.semosan.api.domain.tracking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingSessionActivityServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TrackingSessionActivityService activityService;

    @Test
    void markActiveStoresCurrentTimeWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        activityService.markActive(1L);

        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("tracking:session:1:lastActive"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofHours(25))
        );
    }

    @Test
    void getLastActiveReturnsParsedValue() {
        LocalDateTime lastActive = LocalDateTime.now().minusMinutes(3).withNano(0);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tracking:session:1:lastActive")).thenReturn(lastActive.toString());

        Optional<LocalDateTime> result = activityService.getLastActive(1L);

        assertThat(result).contains(lastActive);
    }

    @Test
    void getLastActiveReturnsEmptyWhenValueIsMissingOrInvalid() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tracking:session:1:lastActive")).thenReturn(null);
        when(valueOperations.get("tracking:session:2:lastActive")).thenReturn("not-date");

        assertThat(activityService.getLastActive(1L)).isEmpty();
        assertThat(activityService.getLastActive(2L)).isEmpty();
    }
}
