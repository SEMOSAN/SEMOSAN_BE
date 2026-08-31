package com.semosan.api.domain.mountain.service;

import com.semosan.api.domain.mountain.entity.Course;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseSummitDistanceCalculatorTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /** 위도 0.001도 ≈ 111.19m (R=6,371,000m 기준). 경도를 고정해 자오선을 따라가면 계산이 결정적이다. */
    private static final double METERS_PER_MILLI_DEGREE_LAT = 111.1949;

    private final CourseSummitDistanceCalculator calculator = new CourseSummitDistanceCalculator();

    @Test
    void calculateAccumulatesDistanceUpToSummitPoint() {
        // 5개 점(4구간) 중 가운데(idx 2)가 정상 — 정상까지는 2구간이다.
        Course course = courseWithSummit(37.002, 127.0, meridianLine(37.0, 127.0, 5));

        Double distance = calculator.calculate(course);

        assertThat(distance).isCloseTo(2 * METERS_PER_MILLI_DEGREE_LAT, within(0.5));
    }

    @Test
    void calculateAccumulatesWholeLineWhenSummitIsTheLastPoint() {
        Course course = courseWithSummit(37.004, 127.0, meridianLine(37.0, 127.0, 5));

        Double distance = calculator.calculate(course);

        assertThat(distance).isCloseTo(4 * METERS_PER_MILLI_DEGREE_LAT, within(0.5));
    }

    @Test
    void calculateSnapsToNearestPointWhenSummitIsOffTheLine() {
        // 관리자가 waypoint 로 수동 지정한 좌표는 polyline 위의 점이 아닐 수 있다.
        // idx 2(37.002) 에서 경도로 살짝 벗어난 좌표를 줘도 같은 인덱스로 스냅되어야 한다.
        Course course = courseWithSummit(37.002, 127.0003, meridianLine(37.0, 127.0, 5));

        Double distance = calculator.calculate(course);

        assertThat(distance).isCloseTo(2 * METERS_PER_MILLI_DEGREE_LAT, within(0.5));
    }

    @Test
    void calculateReturnsNullWhenSummitCoordinateIsMissing() {
        Course course = mock(Course.class);

        assertThat(calculator.calculate(course)).isNull();
    }

    @Test
    void calculateReturnsNullWhenPolylineIsMissing() {
        Course course = courseWithSummit(37.002, 127.0, null);

        assertThat(calculator.calculate(course)).isNull();
    }

    @Test
    void calculateReturnsNullWhenPolylineHasFewerThanTwoPoints() {
        // JTS 는 점 1개짜리 LineString 을 만들 수 없어 빈 LineString 으로 검증한다.
        Course course = courseWithSummit(37.0, 127.0, GEOMETRY_FACTORY.createLineString(new Coordinate[0]));

        assertThat(calculator.calculate(course)).isNull();
    }

    @Test
    void calculateReturnsNullWhenSummitIsTheStartPoint() {
        // 정상이 시작점이면 거리가 0 이라 4등분해도 마일스톤이 의미를 잃는다. 호출자가 fallback 하도록 null.
        Course course = courseWithSummit(37.0, 127.0, meridianLine(37.0, 127.0, 5));

        assertThat(calculator.calculate(course)).isNull();
    }

    @Test
    void calculateReturnsNullWhenCourseIsNull() {
        assertThat(calculator.calculate(null)).isNull();
    }

    private static Course courseWithSummit(Double summitLat, Double summitLng, LineString polyline) {
        Course course = mock(Course.class);
        when(course.getSummitLat()).thenReturn(summitLat);
        when(course.getSummitLng()).thenReturn(summitLng);
        when(course.getPolyline()).thenReturn(polyline);
        return course;
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
