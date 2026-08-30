package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.CourseLikeToggleResponse;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseLikeService {

    private final CourseLikeRepository courseLikeRepository;
    private final CourseRepository courseRepository;
    private final UserReader userReader;

    @Transactional
    public CourseLikeToggleResponse toggleCourseLike(Long userId, Long courseId) {
        boolean liked = toggle(userId, courseId);
        return new CourseLikeToggleResponse(liked);
    }

    private boolean toggle(Long userId, Long courseId) {
        userReader.findActiveUserById(userId);
        findCourseById(courseId);

        return courseLikeRepository.findByUser_IdAndCourse_Id(userId, courseId)
                .map(existing -> {
                    courseLikeRepository.delete(existing);
                    return false;
                })
                // ON CONFLICT DO NOTHING이라 동시 요청이 겹쳐도 예외 없이 0 row로 끝난다.
                .orElseGet(() -> {
                    courseLikeRepository.insertIgnoreConflict(userId, courseId);
                    return true;
                });
    }

    private Course findCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }
}
