package com.semosan.api.domain.tracking.dto.response;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Course;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveActivityCourseResponseTest {

    @Test
    void fromMapsCoursePolylineCoordinates() {
        Course course = mock(Course.class);
        LineString polyline = new GeometryFactory().createLineString(new Coordinate[]{
                new Coordinate(127.0, 37.5),
                new Coordinate(127.1, 37.6)
        });
        when(course.getId()).thenReturn(20L);
        when(course.getPolyline()).thenReturn(polyline);
        when(course.getDistance()).thenReturn(1500.0);
        when(course.getDuration()).thenReturn(90);

        LiveActivityCourseResponse response = LiveActivityCourseResponse.from(course);

        assertThat(response.courseId()).isEqualTo(20L);
        assertThat(response.totalDistance()).isEqualTo(1500.0);
        assertThat(response.estimatedTime()).isEqualTo(90);
        assertThat(response.coordinates())
                .extracting(LiveActivityCourseResponse.CoordinateInfo::latitude)
                .containsExactly(37.5, 37.6);
        assertThat(response.coordinates())
                .extracting(LiveActivityCourseResponse.CoordinateInfo::longitude)
                .containsExactly(127.0, 127.1);
    }

    @Test
    void fromThrowsWhenPolylineIsNull() {
        Course course = mock(Course.class);
        when(course.getPolyline()).thenReturn(null);

        assertThatThrownBy(() -> LiveActivityCourseResponse.from(course))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_COURSE_POLYLINE_REQUIRED);
    }

    @Test
    void fromThrowsWhenPolylineIsEmpty() {
        Course course = mock(Course.class);
        when(course.getPolyline()).thenReturn(new GeometryFactory().createLineString());

        assertThatThrownBy(() -> LiveActivityCourseResponse.from(course))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_COURSE_POLYLINE_REQUIRED);
    }
}
