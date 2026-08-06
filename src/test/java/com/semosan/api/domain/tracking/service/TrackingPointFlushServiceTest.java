package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.tracking.entity.TrackingPoint;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.repository.TrackingPointRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingPointFlushServiceTest {

    @Mock
    private TrackingPointRepository trackingPointRepository;

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @InjectMocks
    private TrackingPointFlushService flushService;

    @Test
    void flushReturnsZeroWhenPendingPointsAreEmpty() {
        int saved = flushService.flush(1L, List.of());

        assertThat(saved).isZero();
        verify(trackingSessionRepository, never()).findById(1L);
        verify(trackingPointRepository, never()).saveAll(anyList());
    }

    @Test
    void flushReturnsZeroWhenSessionDoesNotExist() {
        when(trackingSessionRepository.findById(1L)).thenReturn(Optional.empty());

        int saved = flushService.flush(1L, List.of(point(LocalDateTime.now())));

        assertThat(saved).isZero();
        verify(trackingPointRepository, never()).saveAll(anyList());
    }

    @Test
    void flushSavesOnlyPointsWithinAllowedRecordedAtRange() {
        TrackingSession session = org.mockito.Mockito.mock(TrackingSession.class);
        LocalDateTime now = LocalDateTime.now();
        when(trackingSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        ArgumentCaptor<List<TrackingPoint>> captor = ArgumentCaptor.forClass(List.class);

        int saved = flushService.flush(1L, List.of(
                point(now.minusMinutes(1)),
                point(null),
                point(now.plusMinutes(6)),
                point(now.minusHours(25))
        ));

        assertThat(saved).isEqualTo(1);
        verify(trackingPointRepository).saveAll(captor.capture());
        TrackingPoint trackingPoint = captor.getValue().getFirst();
        assertThat(trackingPoint.getTrackingSession()).isSameAs(session);
        assertThat(trackingPoint.getLocation().getY()).isEqualTo(37.5);
        assertThat(trackingPoint.getLocation().getX()).isEqualTo(127.0);
        assertThat(trackingPoint.getLocation().getSRID()).isEqualTo(4326);
        assertThat(trackingPoint.getAltitude()).isEqualTo(123.4);
        assertThat(trackingPoint.getRecordedAt()).isEqualToIgnoringNanos(now.minusMinutes(1));
    }

    @Test
    void flushReturnsZeroWhenAllPointsAreInvalid() {
        TrackingSession session = org.mockito.Mockito.mock(TrackingSession.class);
        when(trackingSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        int saved = flushService.flush(1L, List.of(
                point(null),
                point(LocalDateTime.now().plusMinutes(6))
        ));

        assertThat(saved).isZero();
        verify(trackingPointRepository, never()).saveAll(anyList());
    }

    private TrackingPointFlushService.PendingPoint point(LocalDateTime recordedAt) {
        return new TrackingPointFlushService.PendingPoint(37.5, 127.0, 123.4, recordedAt);
    }
}
