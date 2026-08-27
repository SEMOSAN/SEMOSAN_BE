package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.tracking.repository.TrackingPointJdbcRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingPointFlushServiceTest {

    @Mock
    private TrackingPointJdbcRepository trackingPointJdbcRepository;

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @InjectMocks
    private TrackingPointFlushService flushService;

    @Test
    void flushReturnsZeroWhenPendingPointsAreEmpty() {
        int saved = flushService.flush(1L, List.of());

        assertThat(saved).isZero();
        verify(trackingSessionRepository, never()).existsById(1L);
        verify(trackingPointJdbcRepository, never()).saveAllInBatch(eq(1L), anyList(), any());
    }

    @Test
    void flushReturnsZeroWhenSessionDoesNotExist() {
        when(trackingSessionRepository.existsById(1L)).thenReturn(false);

        int saved = flushService.flush(1L, List.of(point(LocalDateTime.now())));

        assertThat(saved).isZero();
        verify(trackingPointJdbcRepository, never()).saveAllInBatch(eq(1L), anyList(), any());
    }

    @Test
    void flushSavesOnlyPointsWithinAllowedRecordedAtRange() {
        LocalDateTime now = LocalDateTime.now();
        when(trackingSessionRepository.existsById(1L)).thenReturn(true);
        when(trackingPointJdbcRepository.saveAllInBatch(eq(1L), anyList(), any())).thenReturn(1);
        ArgumentCaptor<List<TrackingPointFlushService.PendingPoint>> captor = ArgumentCaptor.forClass(List.class);

        int saved = flushService.flush(1L, List.of(
                point(now.minusMinutes(1)),
                point(null),
                point(now.plusMinutes(6)),
                point(now.minusHours(25))
        ));

        assertThat(saved).isEqualTo(1);
        verify(trackingPointJdbcRepository).saveAllInBatch(eq(1L), captor.capture(), any());
        TrackingPointFlushService.PendingPoint point = captor.getValue().getFirst();
        assertThat(point.lat()).isEqualTo(37.5);
        assertThat(point.lng()).isEqualTo(127.0);
        assertThat(point.altitude()).isEqualTo(123.4);
        assertThat(point.recordedAt()).isEqualToIgnoringNanos(now.minusMinutes(1));
    }

    @Test
    void flushReturnsZeroWhenAllPointsAreInvalid() {
        when(trackingSessionRepository.existsById(1L)).thenReturn(true);

        int saved = flushService.flush(1L, List.of(
                point(null),
                point(LocalDateTime.now().plusMinutes(6))
        ));

        assertThat(saved).isZero();
        verify(trackingPointJdbcRepository, never()).saveAllInBatch(eq(1L), anyList(), any());
    }

    private TrackingPointFlushService.PendingPoint point(LocalDateTime recordedAt) {
        return new TrackingPointFlushService.PendingPoint(37.5, 127.0, 123.4, recordedAt);
    }
}
