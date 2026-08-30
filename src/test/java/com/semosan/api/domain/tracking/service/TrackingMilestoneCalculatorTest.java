package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrackingMilestoneCalculatorTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /** 위도 0.001도 ≈ 111.19m (R=6,371,000m 기준). 경도를 고정해 자오선을 따라가면 계산이 결정적이다. */
    private static final double METERS_PER_MILLI_DEGREE_LAT = 111.1949;

    private final TrackingMilestoneCalculator calculator = new TrackingMilestoneCalculator();

    @Test
    void calculateSplitsDistanceToSummitWhenCourseHasSummitCoordinate() {
        // 5개 점(4구간) 중 가운데(idx 2)가 정상 — 정상까지는 2구간이다.
        Course course = mock(Course.class);
        when(course.getSummitLat()).thenReturn(37.002);
        when(course.getSummitLng()).thenReturn(127.0);
        when(course.getPolyline()).thenReturn(meridianLine(37.0, 127.0, 5));
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        double expectedSummitDistance = 2 * METERS_PER_MILLI_DEGREE_LAT;
        assertThat(plan.summitMark()).isCloseTo(expectedSummitDistance, within(0.5));
        assertThat(plan.milestones()).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(plan.milestones().get(i))
                    .isCloseTo(expectedSummitDistance * (i + 1) / 4, within(0.5));
        }
        // 4/4 마일스톤이 곧 정상 지점이라 두 값이 같아야 한다.
        assertThat(plan.milestones().get(3)).isEqualTo(plan.summitMark());
    }

    @Test
    void calculateIgnoresCourseDistanceWhenSummitIsResolved() {
        // course.distance 가 정상까지 거리와 무관한 값이어도 마일스톤은 정상 기준으로 잡혀야 한다.
        Course course = mock(Course.class);
        when(course.getSummitLat()).thenReturn(37.004);
        when(course.getSummitLng()).thenReturn(127.0);
        when(course.getPolyline()).thenReturn(meridianLine(37.0, 127.0, 5));
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.summitMark()).isCloseTo(4 * METERS_PER_MILLI_DEGREE_LAT, within(0.5));
    }

    @Test
    void calculateFallsBackToCourseDistanceWhenSummitCoordinateIsMissing() {
        Course course = mock(Course.class);
        when(course.getDistance()).thenReturn(3200.0);
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(800.0, 1600.0, 2400.0, 3200.0);
        // 정상을 모르는 코스는 종전 정책대로 코스 절반을 정상으로 본다.
        assertThat(plan.summitMark()).isEqualTo(1600.0);
    }

    @Test
    void calculateFallsBackWhenPolylineIsMissingEvenThoughSummitExists() {
        Course course = mock(Course.class);
        when(course.getSummitLat()).thenReturn(37.002);
        when(course.getSummitLng()).thenReturn(127.0);
        when(course.getPolyline()).thenReturn(null);
        when(course.getDistance()).thenReturn(1000.0);
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(250.0, 500.0, 750.0, 1000.0);
        assertThat(plan.summitMark()).isEqualTo(500.0);
    }

    @Test
    void calculateFallsBackWhenSummitIsTheCourseStartPoint() {
        // 정상이 시작점이면 4등분해도 전부 0 이라 마일스톤이 의미를 잃는다.
        Course course = mock(Course.class);
        when(course.getSummitLat()).thenReturn(37.0);
        when(course.getSummitLng()).thenReturn(127.0);
        when(course.getPolyline()).thenReturn(meridianLine(37.0, 127.0, 5));
        when(course.getDistance()).thenReturn(2000.0);
        TrackingSession session = sessionWith(course);

        TrackingMilestoneCalculator.MilestonePlan plan = calculator.calculate(session);

        assertThat(plan.milestones()).containsExactly(500.0, 1000.0, 1500.0, 2000.0);
        assertThat(plan.summitMark()).isEqualTo(1000.0);
    }

    @Test
    void calculateReturnsZeroCourseMilestonesWhenCourseDistanceIsNull() {
        Course course = mock(Course.class);
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

    /** 경도를 고정하고 위도만 0.001도씩 올린 LineString — 구간 길이가 모두 같아 기대값 계산이 쉽다. */
    private static LineString meridianLine(double startLat, double lng, int pointCount) {
        Coordinate[] coordinates = new Coordinate[pointCount];
        for (int i = 0; i < pointCount; i++) {
            // JTS Coordinate 는 x=경도, y=위도 순서다.
            coordinates[i] = new Coordinate(lng, startLat + i * 0.001);
        }
        return GEOMETRY_FACTORY.createLineString(coordinates);
    }
}
