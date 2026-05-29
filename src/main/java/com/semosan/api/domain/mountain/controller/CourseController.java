package com.semosan.api.domain.mountain.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.mountain.controller.docs.CourseControllerDocs;
import com.semosan.api.domain.mountain.dto.response.CourseDetailResponse;
import com.semosan.api.domain.mountain.dto.response.CourseLikeToggleResponse;
import com.semosan.api.domain.mountain.service.CourseLikeService;
import com.semosan.api.domain.mountain.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController implements CourseControllerDocs {

    private final CourseService courseService;
    private final CourseLikeService courseLikeService;

    @GetMapping("/{courseId}")
    @Override
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourseDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long courseId
    ) {
        CourseDetailResponse response = courseService.getCourseDetail(userId, courseId);
        return ApiResponse.success(SuccessStatus.COURSE_DETAIL_SUCCESS, response);
    }

    @PostMapping("/{courseId}/like")
    @Override
    public ResponseEntity<ApiResponse<CourseLikeToggleResponse>> toggleCourseLike(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long courseId
    ) {
        return ApiResponse.success(
                SuccessStatus.COURSE_LIKE_TOGGLE_SUCCESS,
                courseLikeService.toggleCourseLike(userId, courseId)
        );
    }
}
