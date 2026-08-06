package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.config.TrackingProperties;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.tracking.dto.message.GpsPointMessage;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingGpsPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private TrackingProperties trackingProperties;

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private TrackingGpsPublisher trackingGpsPublisher;

    @Test
    void publishAddsGpsPointToRedisStreamWhenSessionInProgress() {
        LocalDateTime recordedAt = LocalDateTime.of(2026, 8, 6, 13, 0);
        TrackingSession session = session(10L, 1L, TrackingSessionStatus.IN_PROGRESS);
        when(trackingSessionRepository.findByIdWithUser(10L)).thenReturn(Optional.of(session));
        when(trackingProperties.getStreamKey()).thenReturn("tracking:gps");
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.add(org.mockito.ArgumentMatchers.any(StringRecord.class))).thenReturn(RecordId.of("1-0"));

        trackingGpsPublisher.publish(1L, 10L, new GpsPointMessage(37.5, 127.0, 123.4, recordedAt));

        ArgumentCaptor<StringRecord> captor = ArgumentCaptor.forClass(StringRecord.class);
        verify(streamOperations).add(captor.capture());
        StringRecord record = captor.getValue();
        assertThat(record.getStream()).isEqualTo("tracking:gps");
        assertThat(record.getValue()).containsEntry("sessionId", "10")
                .containsEntry("userId", "1")
                .containsEntry("lat", "37.5")
                .containsEntry("lng", "127.0")
                .containsEntry("altitude", "123.4")
                .containsEntry("recordedAt", recordedAt.toString());
    }

    @Test
    void publishDropsPointWhenSessionIsPaused() {
        TrackingSession session = session(10L, 1L, TrackingSessionStatus.PAUSED);
        when(trackingSessionRepository.findByIdWithUser(10L)).thenReturn(Optional.of(session));

        trackingGpsPublisher.publish(1L, 10L, new GpsPointMessage(37.5, 127.0, null, LocalDateTime.now()));

        verify(redisTemplate, never()).opsForStream();
    }

    @Test
    void publishThrowsWhenSessionMissing() {
        when(trackingSessionRepository.findByIdWithUser(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingGpsPublisher.publish(1L, 10L,
                new GpsPointMessage(37.5, 127.0, null, LocalDateTime.now())))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_NOT_FOUND);
    }

    @Test
    void publishThrowsWhenSessionOwnedByOtherUser() {
        TrackingSession session = session(10L, 2L, TrackingSessionStatus.IN_PROGRESS);
        when(trackingSessionRepository.findByIdWithUser(10L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> trackingGpsPublisher.publish(1L, 10L,
                new GpsPointMessage(37.5, 127.0, null, LocalDateTime.now())))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
    }

    private TrackingSession session(Long id, Long userId, TrackingSessionStatus status) {
        TrackingSession session = org.mockito.Mockito.mock(TrackingSession.class);
        when(session.isOwnedBy(any())).thenAnswer(invocation -> userId.equals(invocation.getArgument(0)));
        lenient().when(session.getStatus()).thenReturn(status);
        return session;
    }
}
