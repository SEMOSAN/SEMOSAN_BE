package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.CourseLike;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseLikeService {

    private final CourseLikeRepository courseLikeRepository;
    private final CourseRepository courseRepository;
    private final UserReader userReader;

    @Transactional
    public void likeCourse(Long userId, Long courseId) {
        User user = userReader.findCompletedOnboardingUserById(userId);
        Course course = findCourseById(courseId);
        if (courseLikeRepository.existsByUser_IdAndCourse_Id(userId, courseId)) {
            throw new GeneralException(ErrorStatus.COURSE_LIKE_ALREADY_EXISTS);
        }

        try {
            courseLikeRepository.save(CourseLike.create(user, course));
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ErrorStatus.COURSE_LIKE_ALREADY_EXISTS);
        }
    }

    @Transactional
    public void unlikeCourse(Long userId, Long courseId) {
        userReader.findCompletedOnboardingUserById(userId);
        CourseLike courseLike = courseLikeRepository.findByUser_IdAndCourse_Id(userId, courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_LIKE_NOT_FOUND));
        courseLikeRepository.delete(courseLike);
    }

    private Course findCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }
}
