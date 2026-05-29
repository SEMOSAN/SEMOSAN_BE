package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.CourseLike;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseLikeServiceTest {

    @Mock
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private CourseLikeService courseLikeService;

    @Test
    void likeCourseSavesCourseLike() throws Exception {
        User user = user(1L);
        Course course = course(10L);

        when(userReader.findCompletedOnboardingUserById(1L)).thenReturn(user);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        courseLikeService.likeCourse(1L, 10L);

        verify(courseLikeRepository).save(any(CourseLike.class));
    }

    @Test
    void likeCourseThrowsWhenAlreadyLiked() throws Exception {
        User user = user(1L);
        Course course = course(10L);

        when(userReader.findCompletedOnboardingUserById(1L)).thenReturn(user);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseLikeRepository.existsByUser_IdAndCourse_Id(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> courseLikeService.likeCourse(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorStatus.COURSE_LIKE_ALREADY_EXISTS.getMessage());
    }

    @Test
    void likeCourseThrowsWhenConcurrentDuplicateDetected() throws Exception {
        User user = user(1L);
        Course course = course(10L);

        when(userReader.findCompletedOnboardingUserById(1L)).thenReturn(user);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseLikeRepository.save(any(CourseLike.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> courseLikeService.likeCourse(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorStatus.COURSE_LIKE_ALREADY_EXISTS.getMessage());
    }

    @Test
    void unlikeCourseDeletesCourseLike() throws Exception {
        User user = user(1L);
        Course course = course(10L);
        CourseLike courseLike = CourseLike.create(user, course);

        when(userReader.findCompletedOnboardingUserById(1L)).thenReturn(user);
        when(courseLikeRepository.findByUser_IdAndCourse_Id(1L, 10L)).thenReturn(Optional.of(courseLike));

        courseLikeService.unlikeCourse(1L, 10L);

        verify(courseLikeRepository).delete(courseLike);
    }

    @Test
    void unlikeCourseThrowsWhenNotLiked() {
        when(userReader.findCompletedOnboardingUserById(1L)).thenReturn(user(1L));
        when(courseLikeRepository.findByUser_IdAndCourse_Id(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseLikeService.unlikeCourse(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorStatus.COURSE_LIKE_NOT_FOUND.getMessage());
    }

    private User user(Long id) {
        User user = User.createTestUser("user-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Course course(Long id) throws Exception {
        Constructor<Course> constructor = Course.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Course course = constructor.newInstance();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}
