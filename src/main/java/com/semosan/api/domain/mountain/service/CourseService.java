package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.CourseDetailResponse;
import com.semosan.api.domain.mountain.dto.response.SlopeSegmentResponse;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.projection.CourseDetailProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final CourseSlopeSegmentCalculator slopeSegmentCalculator;

    public CourseDetailResponse getCourseDetail(Long userId, Long courseId) {
        CourseDetailProjection course = courseRepository.findCourseDetailById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
        boolean likedByMe = userId != null && courseLikeRepository.existsByUser_IdAndCourse_Id(userId, courseId);
        List<SlopeSegmentResponse> segments = slopeSegmentCalculator.calculate(course.getPolyline(), course.getAltitudes());
        return CourseDetailResponse.from(course, likedByMe, segments);
    }
}
