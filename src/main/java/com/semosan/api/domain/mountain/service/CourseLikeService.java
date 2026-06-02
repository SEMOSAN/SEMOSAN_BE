package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.common.util.LikeConflictHandler;
import com.semosan.api.domain.mountain.dto.response.CourseLikeToggleResponse;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.CourseLike;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseLikeService {

    private final CourseLikeRepository courseLikeRepository;
    private final CourseRepository courseRepository;
    private final UserReader userReader;

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public CourseLikeToggleResponse toggleCourseLike(Long userId, Long courseId) {
        boolean liked = toggle(userId, courseId);
        return new CourseLikeToggleResponse(liked);
    }

    private boolean toggle(Long userId, Long courseId) {
        User user = userReader.findCompletedOnboardingUserById(userId);
        Course course = findCourseById(courseId);

        return courseLikeRepository.findByUser_IdAndCourse_Id(userId, courseId)
                .map(existing -> {
                    courseLikeRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> createCourseLike(user, course));
    }

    private boolean createCourseLike(User user, Course course) {
        return LikeConflictHandler.handleConcurrentCreate(
                () -> courseLikeRepository.save(CourseLike.create(user, course)),
                () -> log.warn("CourseLike 동시 요청 감지: courseId={}, userId={}", course.getId(), user.getId())
        );
    }

    private Course findCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }
}
