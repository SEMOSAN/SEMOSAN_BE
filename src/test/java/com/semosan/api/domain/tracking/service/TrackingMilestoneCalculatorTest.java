package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrackingMilestoneCalculatorTest {

    private final TrackingMilestoneCalculator calculator = new TrackingMilestoneCalculator();

    @Test
    void calculateReturnsCourseQuarterMilestones() {
        Course course = mock(Course.class);
        TrackingSession session = mock(TrackingSession.class);
        when(course.getDistance()).thenReturn(3200.0);
        when(session.getCourse()).thenReturn(course);
        when(session.getIsFreeRecording()).thenReturn(false);

        List<Double> result = calculator.calculate(session);

        assertThat(result).containsExactly(800.0, 1600.0, 2400.0, 3200.0);
    }

    @Test
    void calculateReturnsZeroCourseMilestonesWhenCourseDistanceIsNull() {
        Course course = mock(Course.class);
        TrackingSession session = mock(TrackingSession.class);
        when(course.getDistance()).thenReturn(null);
        when(session.getCourse()).thenReturn(course);
        when(session.getIsFreeRecording()).thenReturn(false);

        List<Double> result = calculator.calculate(session);

        assertThat(result).containsExactly(0.0, 0.0, 0.0, 0.0);
    }

    @Test
    void calculateReturnsFreeRecordingMilestonesWhenSessionIsFreeRecording() {
        TrackingSession session = mock(TrackingSession.class);
        when(session.getIsFreeRecording()).thenReturn(true);

        List<Double> result = calculator.calculate(session);

        assertThat(result).containsExactly(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0);
    }

    @Test
    void calculateReturnsFreeRecordingMilestonesWhenCourseIsNull() {
        TrackingSession session = mock(TrackingSession.class);
        when(session.getIsFreeRecording()).thenReturn(false);
        when(session.getCourse()).thenReturn(null);

        List<Double> result = calculator.calculate(session);

        assertThat(result).containsExactly(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0);
    }
}
