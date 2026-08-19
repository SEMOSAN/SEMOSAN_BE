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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
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

    @Test
    void getLastActiveBySessionIdsReturnsParsedValuesBySessionId() {
        LocalDateTime first = LocalDateTime.now().minusMinutes(3).withNano(0);
        LocalDateTime second = LocalDateTime.now().minusMinutes(1).withNano(0);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(List.of(
                "tracking:session:1:lastActive",
                "tracking:session:2:lastActive"
        ))).thenReturn(List.of(first.toString(), second.toString()));

        Map<Long, LocalDateTime> result = activityService.getLastActiveBySessionIds(List.of(1L, 2L));

        assertThat(result).containsExactly(
                Map.entry(1L, first),
                Map.entry(2L, second)
        );
    }

    @Test
    void getLastActiveBySessionIdsExcludesMissingAndInvalidValues() {
        LocalDateTime lastActive = LocalDateTime.now().minusMinutes(3).withNano(0);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(List.of(
                "tracking:session:1:lastActive",
                "tracking:session:2:lastActive",
                "tracking:session:3:lastActive"
        ))).thenReturn(Arrays.asList(lastActive.toString(), "not-date", null));

        Map<Long, LocalDateTime> result = activityService.getLastActiveBySessionIds(List.of(1L, 2L, 3L));

        assertThat(result).containsExactly(Map.entry(1L, lastActive));
    }

    @Test
    void getLastActiveBySessionIdsReturnsEmptyWithoutRedisCallWhenSessionIdsEmpty() {
        Map<Long, LocalDateTime> result = activityService.getLastActiveBySessionIds(List.of());

        assertThat(result).isEmpty();
        verify(redisTemplate, never()).opsForValue();
    }
}
