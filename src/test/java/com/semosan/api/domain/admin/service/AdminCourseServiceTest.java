package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminCourseCreateRequest;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceTest {

    /** 위도 0.001도 ≈ 111.19m (R=6,371,000m 기준). 경도를 고정하면 기대 거리가 결정적이다. */
    private static final double METERS_PER_MILLI_DEGREE_LAT = 111.1949;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MountainRepository mountainRepository;

    @InjectMocks
    private AdminCourseService adminCourseService;

    @Test
    void createBuildsPolylineWithSrid4326AndServerCalculatedDistance() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        adminCourseService.create(request(1L, meridianPoints(37.0, 127.0, 5), null));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        // geography(LineString, 4326) 컬럼과 맞아야 한다. 0 이면 이후 공간 쿼리가 전부 어긋난다.
        assertThat(saved.getPolyline().getSRID()).isEqualTo(4326);
        assertThat(saved.getPolyline().getNumPoints()).isEqualTo(5);
        // JTS 는 x=경도, y=위도 순서다.
        assertThat(saved.getPolyline().getCoordinateN(0).x).isEqualTo(127.0);
        assertThat(saved.getPolyline().getCoordinateN(0).y).isEqualTo(37.0);
        // 4구간 × 0.001도 — 클라이언트가 보낸 값이 아니라 서버가 계산한 값이어야 한다.
        assertThat(saved.getDistance()).isCloseTo(4 * METERS_PER_MILLI_DEGREE_LAT, within(0.5));
    }

    @Test
    void createLeavesAltitudeFieldsNullBecauseMapClicksHaveNoElevation() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        adminCourseService.create(request(1L, meridianPoints(37.0, 127.0, 3), null));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertThat(saved.getAltitudes()).isNull();
        assertThat(saved.getAscent()).isNull();
        assertThat(saved.getDescent()).isNull();
        assertThat(saved.getMaxAltitude()).isNull();
        assertThat(saved.getWaypoints()).isNull();
    }

    @Test
    void createAppliesSummitFromSelectedPointWithNullElevation() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        adminCourseService.create(request(1L, meridianPoints(37.0, 127.0, 5), 2));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertThat(saved.getSummitLat()).isEqualTo(37.002);
        assertThat(saved.getSummitLng()).isEqualTo(127.0);
        // 고도를 모르므로 null 이다.
        assertThat(saved.getSummitEle()).isNull();
    }

    @Test
    void createLeavesSummitUnsetWhenIndexIsNull() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        adminCourseService.create(request(1L, meridianPoints(37.0, 127.0, 5), null));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        // 정상 미지정 — 마일스톤이 정상 기준이 아니라 거리 4등분으로 fallback 된다.
        assertThat(captor.getValue().getSummitLat()).isNull();
    }

    @Test
    void createThrowsWhenSummitIndexIsOutOfRange() {
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain(1L)));

        assertThatThrownBy(() -> adminCourseService.create(request(1L, meridianPoints(37.0, 127.0, 3), 3)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ADMIN_COURSE_SUMMIT_INDEX_INVALID);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenSummitIndexIsNegative() {
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain(1L)));

        assertThatThrownBy(() -> adminCourseService.create(request(1L, meridianPoints(37.0, 127.0, 3), -1)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ADMIN_COURSE_SUMMIT_INDEX_INVALID);
    }

    @Test
    void createThrowsWhenMountainDoesNotExist() {
        when(mountainRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCourseService.create(request(99L, meridianPoints(37.0, 127.0, 3), null)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createTrimsNameAndBlankEndpointNamesBecomeNull() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        adminCourseService.create(new AdminCourseCreateRequest(
                1L, "  테스트 코스  ", Difficulty.EASY, 60,
                meridianPoints(37.0, 127.0, 3), null, "   ", null));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("테스트 코스");
        assertThat(captor.getValue().getStartName()).isNull();
    }

    @Test
    void deleteThrowsWhenCourseDoesNotExist() {
        when(courseRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCourseService.delete(10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COURSE_NOT_FOUND);
    }

    @Test
    void deleteRejectsCourseThatOtherRowsStillReference() {
        Course course = mock(Course.class);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        // courses 참조 FK 들이 ON DELETE 없이 선언돼 있어 참조가 있으면 DB 가 막는다.
        doThrow(new DataIntegrityViolationException("fk violation")).when(courseRepository).flush();

        assertThatThrownBy(() -> adminCourseService.delete(10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ADMIN_COURSE_IN_USE);
    }

    @Test
    void deleteRemovesCourseWhenNothingReferencesIt() {
        Course course = mock(Course.class);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        adminCourseService.delete(10L);

        verify(courseRepository).delete(course);
    }

    /** 경도를 고정하고 위도만 0.001도씩 올린 좌표 목록 — 구간 길이가 같아 기대값 계산이 쉽다. */
    private static List<AdminCourseCreateRequest.PointRequest> meridianPoints(double startLat, double lng, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new AdminCourseCreateRequest.PointRequest(startLat + i * 0.001, lng))
                .toList();
    }

    private static AdminCourseCreateRequest request(
            Long mountainId, List<AdminCourseCreateRequest.PointRequest> points, Integer summitIndex) {
        return new AdminCourseCreateRequest(
                mountainId, "테스트 코스", Difficulty.EASY, 60, points, summitIndex, null, null);
    }

    /** 서비스는 산 엔티티를 Course 에 그대로 넘기기만 하고 필드를 읽지 않는다. */
    private static Mountain mountain(Long id) {
        return mock(Mountain.class);
    }
}
