package com.semosan.api.domain.tracking.scheduler;

import com.semosan.api.common.config.TrackingProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingStreamTrimSchedulerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private TrackingProperties trackingProperties;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private TrackingStreamTrimScheduler scheduler;

    @Test
    void trimStreamTrimsConfiguredStreamApproximately() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(trackingProperties.getStreamKey()).thenReturn("tracking:gps");
        when(trackingProperties.getStreamMaxLen()).thenReturn(1000L);
        when(streamOperations.trim("tracking:gps", 1000L, true)).thenReturn(3L);

        scheduler.trimStream();

        verify(streamOperations).trim("tracking:gps", 1000L, true);
    }

    @Test
    void trimStreamSwallowsRedisFailure() {
        when(redisTemplate.opsForStream()).thenThrow(new IllegalStateException("redis down"));

        scheduler.trimStream();
    }
}
