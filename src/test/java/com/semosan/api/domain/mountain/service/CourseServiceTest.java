package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.CourseDetailResponse;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.projection.CourseDetailProjection;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private CourseDetailProjection projection;

    @InjectMocks
    private CourseService courseService;

    @Test
    void getCourseDetailIncludesLikedByMe() {
        when(userReader.findCompletedOnboardingUserById(1L)).thenReturn(user(1L));
        when(courseRepository.findCourseDetailById(10L)).thenReturn(Optional.of(projection));
        when(courseLikeRepository.existsByUser_IdAndCourse_Id(1L, 10L)).thenReturn(true);
        stubProjection();

        CourseDetailResponse response = courseService.getCourseDetail(1L, 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.mountainId()).isEqualTo(100L);
        assertThat(response.likedByMe()).isTrue();
    }

    @Test
    void getCourseDetailThrowsWhenCourseNotFound() {
        when(userReader.findCompletedOnboardingUserById(1L)).thenReturn(user(1L));
        when(courseRepository.findCourseDetailById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorStatus.COURSE_NOT_FOUND.getMessage());
    }

    private void stubProjection() {
        when(projection.getId()).thenReturn(10L);
        when(projection.getMountainId()).thenReturn(100L);
        when(projection.getName()).thenReturn("가리산 코스 1");
        when(projection.getDifficulty()).thenReturn("EASY");
        when(projection.getDistance()).thenReturn(3300.0);
        when(projection.getDuration()).thenReturn(55);
        when(projection.getStartName()).thenReturn("가리산자연휴양림");
        when(projection.getEndName()).thenReturn("갈림길");
        when(projection.getAscent()).thenReturn(59.0);
        when(projection.getDescent()).thenReturn(593.0);
        when(projection.getMaxAltitude()).thenReturn(930.0);
        when(projection.getPolyline()).thenReturn(null);
        when(projection.getAltitudes()).thenReturn(null);
    }

    private User user(Long id) {
        User user = User.createTestUser("user-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
