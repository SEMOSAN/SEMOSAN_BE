package com.semosan.api.domain.hiking.entity;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HikingRecordTest {

    @Test
    void fromTrackingSessionCreatesRecordWithStatsAndNetDuration() {
        Mountain mountain = mock(Mountain.class);
        Course course = mock(Course.class);
        User user = mock(User.class);
        when(user.getWeight()).thenReturn(70.0);
        TrackingSession session = session(
                user,
                mountain,
                course,
                LocalDateTime.of(2026, 8, 6, 10, 0),
                LocalDateTime.of(2026, 8, 6, 11, 0),
                600
        );

        HikingRecord record = HikingRecord.fromTrackingSession(session, 1000.0, 650.0, 100.0, 80.0, "북한산 아침 산책");

        assertThat(record.getMountain()).isSameAs(mountain);
        assertThat(record.getCourse()).isSameAs(course);
        assertThat(record.getTrackingSession()).isSameAs(session);
        assertThat(record.getDuration()).isEqualTo(3000);
        assertThat(record.getMaxAltitude()).isEqualTo(650.0);
        assertThat(record.getCalories()).isPositive();
        assertThat(record.getDistance()).isEqualTo(1000.0);
        assertThat(record.getAscent()).isEqualTo(100.0);
        assertThat(record.getDescent()).isEqualTo(80.0);
        assertThat(record.getPausedSecondsTotal()).isEqualTo(600);
        assertThat(record.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 8, 6, 10, 0));
        assertThat(record.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 8, 6, 11, 0));
        assertThat(record.getName()).isEqualTo("북한산 아침 산책");
    }

    @Test
    void fromTrackingSessionUsesDefaultsWhenNullableValuesAreMissing() {
        TrackingSession session = session(
                null,
                mock(Mountain.class),
                null,
                LocalDateTime.of(2026, 8, 6, 10, 0),
                LocalDateTime.of(2026, 8, 6, 10, 30),
                null
        );

        HikingRecord record = HikingRecord.fromTrackingSession(session, null, null, null, null, null);

        assertThat(record.getName()).isNull();
        assertThat(record.getCourse()).isNull();
        assertThat(record.getDuration()).isEqualTo(1800);
        assertThat(record.getMaxAltitude()).isZero();
        assertThat(record.getPausedSecondsTotal()).isZero();
        assertThat(record.getDistance()).isNull();
        assertThat(record.getAscent()).isNull();
        assertThat(record.getDescent()).isNull();
    }

    @Test
    void fromTrackingSessionUsesZeroDurationWhenStartOrEndIsMissing() {
        TrackingSession withoutStartedAt = session(
                mock(User.class),
                mock(Mountain.class),
                null,
                null,
                LocalDateTime.of(2026, 8, 6, 10, 30),
                0
        );
        TrackingSession withoutEndedAt = session(
                mock(User.class),
                mock(Mountain.class),
                null,
                LocalDateTime.of(2026, 8, 6, 10, 0),
                null,
                0
        );

        assertThat(HikingRecord.fromTrackingSession(withoutStartedAt, 1000.0, 100.0, 10.0, 0.0, null).getDuration())
                .isZero();
        assertThat(HikingRecord.fromTrackingSession(withoutEndedAt, 1000.0, 100.0, 10.0, 0.0, null).getDuration())
                .isZero();
    }

    @Test
    void fromTrackingSessionDoesNotReturnNegativeDurationWhenPausedTimeExceedsTotal() {
        TrackingSession session = session(
                mock(User.class),
                mock(Mountain.class),
                null,
                LocalDateTime.of(2026, 8, 6, 10, 0),
                LocalDateTime.of(2026, 8, 6, 10, 1),
                120
        );

        HikingRecord record = HikingRecord.fromTrackingSession(session, 1000.0, 100.0, 10.0, 0.0, null);

        assertThat(record.getDuration()).isZero();
    }

    @Test
    void updateTemperatureChangesTemperature() {
        TrackingSession session = session(
                mock(User.class),
                mock(Mountain.class),
                null,
                LocalDateTime.of(2026, 8, 6, 10, 0),
                LocalDateTime.of(2026, 8, 6, 10, 30),
                0
        );
        HikingRecord record = HikingRecord.fromTrackingSession(session, 1000.0, 100.0, 10.0, 0.0, null);

        record.updateTemperature(18.5);

        assertThat(record.getTemperature()).isEqualTo(18.5);
    }

    private TrackingSession session(
            User user,
            Mountain mountain,
            Course course,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Integer pausedSecondsTotal
    ) {
        TrackingSession session = mock(TrackingSession.class);
        when(session.getUser()).thenReturn(user);
        when(session.getMountain()).thenReturn(mountain);
        when(session.getCourse()).thenReturn(course);
        when(session.getStartedAt()).thenReturn(startedAt);
        when(session.getEndedAt()).thenReturn(endedAt);
        when(session.getPausedSecondsTotal()).thenReturn(pausedSecondsTotal);
        return session;
    }
}
