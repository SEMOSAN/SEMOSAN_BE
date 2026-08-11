package com.semosan.api.domain.tracking.scheduler;

import com.semosan.api.domain.tracking.service.TrackingPointCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingPointCleanupSchedulerTest {

    private static final TrackingPointCleanupService.BatchResult EMPTY =
            new TrackingPointCleanupService.BatchResult(0, 0);
    private static final TrackingPointCleanupService.BatchResult FULL =
            new TrackingPointCleanupService.BatchResult(100, 100_000);

    @Mock
    private TrackingPointCleanupService cleanupService;

    @InjectMocks
    private TrackingPointCleanupScheduler scheduler;

    @Test
    void stopsImmediatelyWhenNothingToClean() {
        when(cleanupService.cleanupBatch(any(), anyInt())).thenReturn(EMPTY);

        scheduler.cleanupOrphanedPoints();

        verify(cleanupService, times(1)).cleanupBatch(any(), anyInt());
    }

    @Test
    void repeatsUntilBatchComesBackEmpty() {
        when(cleanupService.cleanupBatch(any(), anyInt()))
                .thenReturn(new TrackingPointCleanupService.BatchResult(100, 120_000))
                .thenReturn(new TrackingPointCleanupService.BatchResult(40, 48_000))
                .thenReturn(EMPTY);

        scheduler.cleanupOrphanedPoints();

        verify(cleanupService, times(3)).cleanupBatch(any(), anyInt());
    }

    @Test
    void stopsAtMaxBatchesWhenBacklogIsLarge() {
        // 계속 가득 찬 배치가 돌아오는 상황 — 무한 루프 없이 상한에서 멈춰야 한다.
        when(cleanupService.cleanupBatch(any(), anyInt())).thenReturn(FULL);

        scheduler.cleanupOrphanedPoints();

        verify(cleanupService, times(50)).cleanupBatch(any(), anyInt());
    }

    @Test
    void stopsWithoutPropagatingWhenBatchFails() {
        when(cleanupService.cleanupBatch(any(), anyInt()))
                .thenReturn(FULL)
                .thenThrow(new RuntimeException("db down"));

        // 스케줄러가 예외를 던지면 다음 주기까지 로그만 남고 원인 추적이 어려워진다.
        assertThatCode(scheduler::cleanupOrphanedPoints).doesNotThrowAnyException();
        verify(cleanupService, times(2)).cleanupBatch(any(), anyInt());
    }

    @Test
    void passesCutoffOneDayBeforeNow() {
        when(cleanupService.cleanupBatch(any(), anyInt())).thenReturn(EMPTY);
        LocalDateTime before = LocalDateTime.now().minusDays(1);

        scheduler.cleanupOrphanedPoints();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(cleanupService).cleanupBatch(captor.capture(), anyInt());
        assertThat(captor.getValue())
                .isBetween(before.minusSeconds(5), LocalDateTime.now().minusDays(1).plusSeconds(5));
    }
}
