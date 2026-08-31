package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.service.CourseSummitDistanceCalculator;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 정상까지 거리를 실제로 구하는 로직은 {@link CourseSummitDistanceCalculator} 쪽 테스트가 담당한다.
 * 여기서는 그 값을 받아 마일스톤으로 쪼개는 규칙만 검증한다.
 */
class TrackingMilestoneCalculatorTest {

    private final CourseSummitDistanceCalculator summitDistanceCalculator = mock(CourseSummitDistanceCalculator.class);
    private final TrackingMilestoneCalculator calculator = new TrackingMilestoneCalculator(summitDistanceCalculator);

    @Test
    void calculateSplitsDistanceToSummitWhenSummitIsResolved() {
        Course course = mock(Course.class);
        when(summitDistanceCalculator.calculate(course)).thenReturn(2000.0);
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(500.0, 1000.0, 1500.0, 2000.0);
        // 4/4 마일스톤이 곧 정상 지점이라 두 값이 같아야 한다.
        assertThat(plan.summitMark()).isEqualTo(2000.0);
        assertThat(plan.milestones().get(3)).isEqualTo(plan.summitMark());
    }

    @Test
    void calculateIgnoresCourseDistanceWhenSummitIsResolved() {
        // course.distance 가 정상까지 거리와 무관해도 마일스톤은 정상 기준으로 잡혀야 한다.
        Course course = mock(Course.class);
        when(summitDistanceCalculator.calculate(course)).thenReturn(1200.0);
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(300.0, 600.0, 900.0, 1200.0);
    }

    @Test
    void calculateFallsBackToCourseDistanceWhenSummitDistanceIsNull() {
        Course course = mock(Course.class);
        when(summitDistanceCalculator.calculate(course)).thenReturn(null);
        when(course.getDistance()).thenReturn(3200.0);
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(800.0, 1600.0, 2400.0, 3200.0);
        // 정상을 모르는 코스는 종전 정책대로 코스 절반을 정상으로 본다 — 2/4 마일스톤과 같은 지점이다.
        assertThat(plan.summitMark()).isEqualTo(1600.0);
        assertThat(plan.milestones().get(1)).isEqualTo(plan.summitMark());
    }

    @Test
    void calculateFallsBackWhenSummitDistanceIsZero() {
        Course course = mock(Course.class);
        when(summitDistanceCalculator.calculate(course)).thenReturn(0.0);
        when(course.getDistance()).thenReturn(1000.0);
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(250.0, 500.0, 750.0, 1000.0);
        assertThat(plan.summitMark()).isEqualTo(500.0);
    }

    @Test
    void calculateReturnsZeroCourseMilestonesWhenCourseDistanceIsNull() {
        Course course = mock(Course.class);
        when(summitDistanceCalculator.calculate(course)).thenReturn(null);
        when(course.getDistance()).thenReturn(null);
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(0.0, 0.0, 0.0, 0.0);
        assertThat(plan.summitMark()).isEqualTo(0.0);
    }

    @Test
    void calculateReturnsFourFreeRecordingMilestonesWhenSessionIsFreeRecording() {
        TrackingSession session = mock(TrackingSession.class);
        when(session.getIsFreeRecording()).thenReturn(true);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(500.0, 1000.0, 1500.0, 2000.0);
        // 자유 기록엔 정상이 없다. null 여부가 코스 모드 판별 기준이라 반드시 null 이어야 한다.
        assertThat(plan.summitMark()).isNull();
    }

    @Test
    void calculateReturnsFreeRecordingMilestonesWhenCourseIsNull() {
        TrackingSession session = mock(TrackingSession.class);
        when(session.getIsFreeRecording()).thenReturn(false);
        when(session.getCourse()).thenReturn(null);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(500.0, 1000.0, 1500.0, 2000.0);
        assertThat(plan.summitMark()).isNull();
    }

    private static TrackingSession sessionWith(Course course) {
        TrackingSession session = mock(TrackingSession.class);
        when(session.getIsFreeRecording()).thenReturn(false);
        when(session.getCourse()).thenReturn(course);
        return session;
    }
}
