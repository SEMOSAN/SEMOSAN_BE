package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.tracking.event.TrackingSessionTerminatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingStreamConsumerTest {

    @Mock
    private TrackingSessionStatsService statsService;

    @Mock
    private TrackingPointFlushService flushService;

    @Mock
    private TrackingMilestoneTriggerService milestoneTriggerService;

    @Mock
    private TrackingSessionActivityService activityService;

    @InjectMocks
    private TrackingStreamConsumer consumer;

    @Test
    void onMessageRecordsStatsEvaluatesMilestonesAndBuffersPoint() {
        LocalDateTime recordedAt = LocalDateTime.now();
        when(statsService.getLastPosition(1L))
                .thenReturn(new TrackingSessionStatsService.LastPosition(null, null, null));
        when(statsService.recordPoint(1L, 37.5, 127.0, 100.0, recordedAt)).thenReturn(120.0);

        consumer.onMessage(message(1L, 10L, 37.5, 127.0, "100.0", recordedAt));
        consumer.flushAll();

        verify(activityService).markActive(1L);
        verify(statsService).recordPoint(1L, 37.5, 127.0, 100.0, recordedAt);
        verify(milestoneTriggerService).evaluate(1L, 10L, 120.0);
        ArgumentCaptor<List<TrackingPointFlushService.PendingPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(flushService).flush(eq(1L), captor.capture());
        assertThat(captor.getValue()).singleElement()
                .satisfies(point -> {
                    assertThat(point.lat()).isEqualTo(37.5);
                    assertThat(point.lng()).isEqualTo(127.0);
                    assertThat(point.altitude()).isEqualTo(100.0);
                    assertThat(point.recordedAt()).isEqualTo(recordedAt);
                });
    }

    @Test
    void onMessageSkipsNoisyPointWithinDistanceAndAltitudeThreshold() {
        LocalDateTime recordedAt = LocalDateTime.now();
        when(statsService.getLastPosition(1L))
                .thenReturn(new TrackingSessionStatsService.LastPosition(37.5, 127.0, 100.0));

        consumer.onMessage(message(1L, 10L, 37.50001, 127.00001, "101.0", recordedAt));

        verify(activityService).markActive(1L);
        verify(statsService, never()).recordPoint(any(), any(Double.class), any(Double.class), any(), any());
        verify(milestoneTriggerService, never()).evaluate(any(), any(), any(Double.class));
        verify(flushService, never()).flush(any(), any());
    }

    @Test
    void onMessageAcceptsPointWhenHorizontalDistancePassesThreshold() {
        LocalDateTime recordedAt = LocalDateTime.now();
        when(statsService.getLastPosition(1L))
                .thenReturn(new TrackingSessionStatsService.LastPosition(37.5, 127.0, 100.0));
        when(statsService.recordPoint(1L, 37.501, 127.0, 101.0, recordedAt)).thenReturn(20.0);

        consumer.onMessage(message(1L, 10L, 37.501, 127.0, "101.0", recordedAt));

        verify(statsService).recordPoint(1L, 37.501, 127.0, 101.0, recordedAt);
        verify(milestoneTriggerService).evaluate(1L, 10L, 20.0);
    }

    @Test
    void onMessageAcceptsPointWhenOnlyAltitudeChangePassesThreshold() {
        LocalDateTime recordedAt = LocalDateTime.now();
        when(statsService.getLastPosition(1L))
                .thenReturn(new TrackingSessionStatsService.LastPosition(37.5, 127.0, 100.0));
        when(statsService.recordPoint(1L, 37.50001, 127.00001, 104.0, recordedAt)).thenReturn(15.0);

        consumer.onMessage(message(1L, 10L, 37.50001, 127.00001, "104.0", recordedAt));

        verify(statsService).recordPoint(1L, 37.50001, 127.00001, 104.0, recordedAt);
        verify(milestoneTriggerService).evaluate(1L, 10L, 15.0);
    }

    @Test
    void flushAllRequeuesBatchWhenFlushFailsAndRetriesNextFlush() {
        LocalDateTime recordedAt = LocalDateTime.now();
        when(statsService.getLastPosition(1L))
                .thenReturn(new TrackingSessionStatsService.LastPosition(null, null, null));
        when(statsService.recordPoint(1L, 37.5, 127.0, null, recordedAt)).thenReturn(10.0);
        when(flushService.flush(eq(1L), any())).thenThrow(new RuntimeException("db down")).thenReturn(1);

        consumer.onMessage(message(1L, 10L, 37.5, 127.0, "", recordedAt));
        consumer.flushAll();
        consumer.flushAll();

        verify(flushService, times(2)).flush(eq(1L), any());
    }

    @Test
    void onMessageFlushesImmediatelyWhenBufferReachesThreshold() {
        LocalDateTime recordedAt = LocalDateTime.now();
        when(statsService.getLastPosition(1L))
                .thenReturn(new TrackingSessionStatsService.LastPosition(null, null, null));
        when(statsService.recordPoint(eq(1L), any(Double.class), any(Double.class), any(), eq(recordedAt)))
                .thenReturn(10.0);
        when(flushService.flush(eq(1L), any())).thenReturn(100);

        for (int i = 0; i < 100; i++) {
            consumer.onMessage(message(1L, 10L, 37.5 + i * 0.001, 127.0, "", recordedAt));
        }

        verify(flushService).flush(eq(1L), any());
    }

    @Test
    void onSessionTerminatedReturnsWhenBufferIsMissing() {
        consumer.onSessionTerminated(new TrackingSessionTerminatedEvent(999L));

        verify(flushService, never()).flush(any(), any());
    }

    @Test
    void onSessionTerminatedFlushesRemainingBufferedPointsAndClearsBuffer() {
        LocalDateTime recordedAt = LocalDateTime.now();
        when(statsService.getLastPosition(1L))
                .thenReturn(new TrackingSessionStatsService.LastPosition(null, null, null));
        when(statsService.recordPoint(1L, 37.5, 127.0, null, recordedAt)).thenReturn(10.0);

        consumer.onMessage(message(1L, 10L, 37.5, 127.0, "", recordedAt));
        consumer.onSessionTerminated(new TrackingSessionTerminatedEvent(1L));
        consumer.flushAll();

        verify(flushService).flush(eq(1L), any());
    }

    @Test
    void onSessionTerminatedSuppressesFinalFlushFailureAndClearsBuffer() {
        LocalDateTime recordedAt = LocalDateTime.now();
        when(statsService.getLastPosition(1L))
                .thenReturn(new TrackingSessionStatsService.LastPosition(null, null, null));
        when(statsService.recordPoint(1L, 37.5, 127.0, null, recordedAt)).thenReturn(10.0);
        when(flushService.flush(eq(1L), any())).thenThrow(new RuntimeException("db down"));

        consumer.onMessage(message(1L, 10L, 37.5, 127.0, "", recordedAt));
        consumer.onSessionTerminated(new TrackingSessionTerminatedEvent(1L));
        consumer.flushAll();

        verify(flushService).flush(eq(1L), any());
    }

    @Test
    void onMessageIgnoresMalformedMessage() {
        consumer.onMessage(MapRecord.create("tracking:gps", Map.of("sessionId", "bad")));

        verify(activityService, never()).markActive(any());
        verify(statsService, never()).recordPoint(any(), any(Double.class), any(Double.class), any(), any());
        verify(flushService, never()).flush(any(), any());
    }

    private MapRecord<String, String, String> message(
            Long sessionId,
            Long userId,
            double lat,
            double lng,
            String altitude,
            LocalDateTime recordedAt
    ) {
        return MapRecord.create("tracking:gps", Map.of(
                "sessionId", String.valueOf(sessionId),
                "userId", String.valueOf(userId),
                "lat", String.valueOf(lat),
                "lng", String.valueOf(lng),
                "altitude", altitude,
                "recordedAt", recordedAt.toString()
        ));
    }
}
