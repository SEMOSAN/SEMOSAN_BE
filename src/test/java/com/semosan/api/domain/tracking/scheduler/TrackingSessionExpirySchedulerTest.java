package com.semosan.api.domain.tracking.scheduler;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.event.TrackingSessionTerminatedEvent;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import com.semosan.api.domain.tracking.service.TrackingSessionActivityService;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingSessionExpirySchedulerTest {

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TrackingSessionActivityService activityService;

    @InjectMocks
    private TrackingSessionExpiryScheduler scheduler;

    @Test
    void expireStaleSessionsReturnsWhenCandidatesEmpty() {
        when(trackingSessionRepository.findStaleActiveSessions(eq(TrackingSessionStatus.ACTIVE_STATES), any()))
                .thenReturn(List.of());

        scheduler.expireStaleSessions();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void expireStaleSessionsSkipsRecentlyActiveSession() {
        TrackingSession session = session(10L, TrackingSessionStatus.IN_PROGRESS);
        when(trackingSessionRepository.findStaleActiveSessions(eq(TrackingSessionStatus.ACTIVE_STATES), any()))
                .thenReturn(List.of(session));
        when(activityService.getLastActiveBySessionIds(List.of(10L)))
                .thenReturn(Map.of(10L, LocalDateTime.now()));

        scheduler.expireStaleSessions();

        assertThat(session.getStatus()).isEqualTo(TrackingSessionStatus.IN_PROGRESS);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void expireStaleSessionsAbandonsSessionWhenRedisActivityIsMissing() {
        TrackingSession session = session(10L, TrackingSessionStatus.PAUSED);
        when(trackingSessionRepository.findStaleActiveSessions(eq(TrackingSessionStatus.ACTIVE_STATES), any()))
                .thenReturn(List.of(session));
        when(activityService.getLastActiveBySessionIds(List.of(10L))).thenReturn(Map.of());

        scheduler.expireStaleSessions();

        assertThat(session.getStatus()).isEqualTo(TrackingSessionStatus.ABANDONED);
        verify(eventPublisher).publishEvent(any(TrackingSessionTerminatedEvent.class));
    }

    @Test
    void expireStaleSessionsFetchesRedisActivityInBatch() {
        TrackingSession recent = session(10L, TrackingSessionStatus.IN_PROGRESS);
        TrackingSession stale = session(11L, TrackingSessionStatus.IN_PROGRESS);
        when(trackingSessionRepository.findStaleActiveSessions(eq(TrackingSessionStatus.ACTIVE_STATES), any()))
                .thenReturn(List.of(recent, stale));
        when(activityService.getLastActiveBySessionIds(List.of(10L, 11L)))
                .thenReturn(Map.of(10L, LocalDateTime.now()));

        scheduler.expireStaleSessions();

        assertThat(recent.getStatus()).isEqualTo(TrackingSessionStatus.IN_PROGRESS);
        assertThat(stale.getStatus()).isEqualTo(TrackingSessionStatus.ABANDONED);
        verify(activityService).getLastActiveBySessionIds(List.of(10L, 11L));
        verify(eventPublisher).publishEvent(any(TrackingSessionTerminatedEvent.class));
    }

    private TrackingSession session(Long id, TrackingSessionStatus status) {
        TrackingSession session = TrackingSession.create(user(), mountain(), null, true);
        ReflectionTestUtils.setField(session, "id", id);
        ReflectionTestUtils.setField(session, "status", status);
        return session;
    }

    private User user() {
        User user = User.createTestUser("test-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Mountain mountain() {
        Mountain mountain = newInstance(Mountain.class);
        ReflectionTestUtils.setField(mountain, "id", 1L);
        ReflectionTestUtils.setField(mountain, "name", "관악산");
        return mountain;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
