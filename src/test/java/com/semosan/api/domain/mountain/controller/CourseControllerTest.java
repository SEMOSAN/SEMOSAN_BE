package com.semosan.api.domain.mountain.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.mountain.dto.response.CourseDetailResponse;
import com.semosan.api.domain.mountain.dto.response.CourseLikeToggleResponse;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.service.CourseLikeService;
import com.semosan.api.domain.mountain.service.CourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    @Mock
    private CourseLikeService courseLikeService;

    @InjectMocks
    private CourseController courseController;

    @Test
    void getCourseDetailReturnsSuccessResponse() {
        CourseDetailResponse detail = new CourseDetailResponse(
                10L, 1L, "정상 코스", Difficulty.NORMAL, 1500.0, 90,
                "입구", "정상", 100.0, 80.0, 650.0,
                true, "{}", "[]", List.of()
        );
        when(courseService.getCourseDetail(1L, 10L)).thenReturn(detail);

        ResponseEntity<ApiResponse<CourseDetailResponse>> response = courseController.getCourseDetail(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.COURSE_DETAIL_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(detail);
    }

    @Test
    void toggleCourseLikeReturnsSuccessResponse() {
        CourseLikeToggleResponse toggleResponse = new CourseLikeToggleResponse(true);
        when(courseLikeService.toggleCourseLike(1L, 10L)).thenReturn(toggleResponse);

        ResponseEntity<ApiResponse<CourseLikeToggleResponse>> response = courseController.toggleCourseLike(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.COURSE_LIKE_TOGGLE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(toggleResponse);
        verify(courseLikeService).toggleCourseLike(1L, 10L);
    }
}
