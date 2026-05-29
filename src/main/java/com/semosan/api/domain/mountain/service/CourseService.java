package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.CourseDetailResponse;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.projection.CourseDetailProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseLikeRepository courseLikeRepository;

    public CourseDetailResponse getCourseDetail(Long userId, Long courseId) {
        CourseDetailProjection course = courseRepository.findCourseDetailById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
        boolean likedByMe = userId != null && courseLikeRepository.existsByUser_IdAndCourse_Id(userId, courseId);
        return CourseDetailResponse.from(course, likedByMe);
    }
}
