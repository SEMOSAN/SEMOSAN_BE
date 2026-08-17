package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.repository.TrackingPointRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingPointCleanupServiceTest {

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @Mock
    private TrackingPointRepository trackingPointRepository;

    @InjectMocks
    private TrackingPointCleanupService trackingPointCleanupService;

    @Test
    void cleanupBatchDeletesPointsOfMatchedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        when(trackingSessionRepository.findSessionIdsForPointCleanup(
                eq(TrackingSessionStatus.ABANDONED), eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(10L, 11L, 12L));
        when(trackingPointRepository.deleteByTrackingSessionIdIn(List.of(10L, 11L, 12L))).thenReturn(3_000);

        TrackingPointCleanupService.BatchResult result =
                trackingPointCleanupService.cleanupBatch(cutoff, 100);

        assertThat(result.sessionCount()).isEqualTo(3);
        assertThat(result.deletedPoints()).isEqualTo(3_000);
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    void cleanupBatchSkipsDeleteWhenNoSessionMatches() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        when(trackingSessionRepository.findSessionIdsForPointCleanup(
                eq(TrackingSessionStatus.ABANDONED), eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of());

        TrackingPointCleanupService.BatchResult result =
                trackingPointCleanupService.cleanupBatch(cutoff, 100);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.deletedPoints()).isZero();
        verifyNoInteractions(trackingPointRepository);
    }

    @Test
    void cleanupBatchRequestsOnlyGivenBatchSize() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        when(trackingSessionRepository.findSessionIdsForPointCleanup(
                any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        trackingPointCleanupService.cleanupBatch(cutoff, 25);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(trackingSessionRepository).findSessionIdsForPointCleanup(any(), any(), captor.capture());
        assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 25));
    }

    @Test
    void cleanupBatchTargetsAbandonedStatusOnly() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        when(trackingSessionRepository.findSessionIdsForPointCleanup(any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        trackingPointCleanupService.cleanupBatch(cutoff, 100);

        // COMPLETED 세션의 좌표는 HikingRecord 경로 폴리라인에 쓰이므로 절대 대상이 되면 안 된다.
        verify(trackingSessionRepository).findSessionIdsForPointCleanup(
                eq(TrackingSessionStatus.ABANDONED), eq(cutoff), any(Pageable.class));
        verify(trackingPointRepository, never()).deleteByTrackingSessionIdIn(any());
    }
}
