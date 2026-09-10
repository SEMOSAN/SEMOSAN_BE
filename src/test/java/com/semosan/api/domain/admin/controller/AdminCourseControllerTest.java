package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.dto.request.AdminCourseCreateRequest;
import com.semosan.api.domain.admin.service.AdminCourseService;
import com.semosan.api.domain.mountain.enums.Difficulty;
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
class AdminCourseControllerTest {

    @Mock
    private AdminCourseService adminCourseService;

    @InjectMocks
    private AdminCourseController adminCourseController;

    @Test
    void createReturnsCreatedCourseId() {
        AdminCourseCreateRequest request = new AdminCourseCreateRequest(
                1L, "테스트 코스", Difficulty.EASY, 60,
                List.of(
                        new AdminCourseCreateRequest.PointRequest(37.0, 127.0),
                        new AdminCourseCreateRequest.PointRequest(37.001, 127.0)
                ),
                1, null, null);
        when(adminCourseService.create(request)).thenReturn(42L);

        ResponseEntity<ApiResponse<Long>> response = adminCourseController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.ADMIN_COURSE_CREATE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isEqualTo(42L);
        verify(adminCourseService).create(request);
    }

    @Test
    void deleteReturnsSuccessResponse() {
        ResponseEntity<ApiResponse<Void>> response = adminCourseController.delete(10L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.ADMIN_COURSE_DELETE_SUCCESS.getHttpStatus());
        verify(adminCourseService).delete(10L);
    }
}
