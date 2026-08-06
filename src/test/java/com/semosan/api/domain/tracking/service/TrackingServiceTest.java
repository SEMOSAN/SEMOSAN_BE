package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.tracking.dto.response.LiveActivityCourseResponse;
import com.semosan.api.domain.tracking.dto.response.NearbyMountainResponse;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private MountainRepository mountainRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private TrackingService trackingService;

    @Test
    void getNearbyMountainReturnsNearestMountainAndCourses() {
        Mountain mountain = mountain(1L);
        Course course = course(10L, mountain);
        when(mountainRepository.findNearestByLatLng(37.5, 127.0)).thenReturn(Optional.of(mountain));
        when(courseRepository.findByMountainId(1L)).thenReturn(List.of(course));

        NearbyMountainResponse response = trackingService.getNearbyMountain(2L, 37.5, 127.0);

        verify(userReader).findActiveUserById(2L);
        assertThat(response.mountain().mountainId()).isEqualTo(1L);
        assertThat(response.mountain().name()).isEqualTo("관악산");
        assertThat(response.courses()).hasSize(1);
        assertThat(response.courses().getFirst().courseId()).isEqualTo(10L);
    }

    @Test
    void getNearbyMountainThrowsWhenMountainMissing() {
        when(mountainRepository.findNearestByLatLng(37.5, 127.0)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.getNearbyMountain(2L, 37.5, 127.0))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void getLiveActivityCourseReturnsPolylineCoordinates() {
        Mountain mountain = mountain(1L);
        Course course = course(10L, mountain);
        LineString polyline = new GeometryFactory(new PrecisionModel(), 4326)
                .createLineString(new Coordinate[]{
                        new Coordinate(127.0, 37.5),
                        new Coordinate(127.1, 37.6)
                });
        ReflectionTestUtils.setField(course, "polyline", polyline);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        LiveActivityCourseResponse response = trackingService.getLiveActivityCourse(2L, 10L);

        verify(userReader).findActiveUserById(2L);
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.coordinates()).hasSize(2);
        assertThat(response.coordinates().getFirst().latitude()).isEqualTo(37.5);
        assertThat(response.coordinates().getFirst().longitude()).isEqualTo(127.0);
        assertThat(response.totalDistance()).isEqualTo(1500.0);
        assertThat(response.estimatedTime()).isEqualTo(90);
    }

    @Test
    void getLiveActivityCourseThrowsWhenCourseMissing() {
        when(courseRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.getLiveActivityCourse(2L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COURSE_NOT_FOUND);
    }

    @Test
    void getLiveActivityCourseThrowsWhenPolylineMissing() {
        Course course = course(10L, mountain(1L));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> trackingService.getLiveActivityCourse(2L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_COURSE_POLYLINE_REQUIRED);
    }

    private Mountain mountain(Long id) {
        Mountain mountain = newInstance(Mountain.class);
        ReflectionTestUtils.setField(mountain, "id", id);
        ReflectionTestUtils.setField(mountain, "name", "관악산");
        ReflectionTestUtils.setField(mountain, "address", "서울 관악구");
        ReflectionTestUtils.setField(mountain, "altitude", 632.2);
        ReflectionTestUtils.setField(mountain, "latitude", 37.5);
        ReflectionTestUtils.setField(mountain, "longitude", 127.0);
        ReflectionTestUtils.setField(mountain, "imageUrls", List.of("image.jpg"));
        return mountain;
    }

    private Course course(Long id, Mountain mountain) {
        Course course = newInstance(Course.class);
        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "mountain", mountain);
        ReflectionTestUtils.setField(course, "name", "정상 코스");
        ReflectionTestUtils.setField(course, "difficulty", Difficulty.NORMAL);
        ReflectionTestUtils.setField(course, "distance", 1500.0);
        ReflectionTestUtils.setField(course, "duration", 90);
        return course;
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
