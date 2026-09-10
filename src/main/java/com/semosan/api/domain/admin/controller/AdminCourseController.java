package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.controller.docs.AdminCourseControllerDocs;
import com.semosan.api.domain.admin.dto.request.AdminCourseCreateRequest;
import com.semosan.api.domain.admin.service.AdminCourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController implements AdminCourseControllerDocs {

    private final AdminCourseService adminCourseService;

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody AdminCourseCreateRequest request
    ) {
        Long courseId = adminCourseService.create(request);
        return ApiResponse.success(SuccessStatus.ADMIN_COURSE_CREATE_SUCCESS, courseId);
    }

    @DeleteMapping("/{courseId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long courseId) {
        adminCourseService.delete(courseId);
        return ApiResponse.success(SuccessStatus.ADMIN_COURSE_DELETE_SUCCESS);
    }
}
