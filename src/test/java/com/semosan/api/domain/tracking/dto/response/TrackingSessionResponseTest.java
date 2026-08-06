package com.semosan.api.domain.tracking.dto.response;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrackingSessionResponseTest {

    @Test
    void fromMapsSessionWithoutCourseAndRecordId() {
        TrackingSession session = session(null);

        TrackingSessionResponse response = TrackingSessionResponse.from(session);

        assertThat(response.sessionId()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.mountainId()).isEqualTo(10L);
        assertThat(response.mountainName()).isEqualTo("관악산");
        assertThat(response.courseId()).isNull();
        assertThat(response.courseName()).isNull();
        assertThat(response.isFreeRecording()).isTrue();
        assertThat(response.status()).isEqualTo(TrackingSessionStatus.IN_PROGRESS);
        assertThat(response.startedAt()).isEqualTo(LocalDateTime.of(2026, 8, 6, 10, 0));
        assertThat(response.hikingRecordId()).isNull();
    }

    @Test
    void fromMapsSessionWithCourseAndRecordId() {
        Course course = mock(Course.class);
        when(course.getId()).thenReturn(20L);
        when(course.getName()).thenReturn("정상 코스");
        TrackingSession session = session(course);

        TrackingSessionResponse response = TrackingSessionResponse.from(session, 300L);

        assertThat(response.courseId()).isEqualTo(20L);
        assertThat(response.courseName()).isEqualTo("정상 코스");
        assertThat(response.hikingRecordId()).isEqualTo(300L);
    }

    private TrackingSession session(Course course) {
        TrackingSession session = mock(TrackingSession.class);
        User user = mock(User.class);
        Mountain mountain = mock(Mountain.class);
        when(session.getId()).thenReturn(100L);
        when(session.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(session.getMountain()).thenReturn(mountain);
        when(mountain.getId()).thenReturn(10L);
        when(mountain.getName()).thenReturn("관악산");
        when(session.getCourse()).thenReturn(course);
        when(session.getIsFreeRecording()).thenReturn(true);
        when(session.getStatus()).thenReturn(TrackingSessionStatus.IN_PROGRESS);
        when(session.getStartedAt()).thenReturn(LocalDateTime.of(2026, 8, 6, 10, 0));
        when(session.getEndedAt()).thenReturn(null);
        when(session.getPausedAt()).thenReturn(null);
        when(session.getPausedSecondsTotal()).thenReturn(0);
        return session;
    }
}
