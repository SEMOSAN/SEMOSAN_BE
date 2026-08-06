package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.dto.request.AdminUserSuspendRequest;
import com.semosan.api.domain.admin.dto.response.AdminReportedPostResponse;
import com.semosan.api.domain.admin.service.AdminCommunityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCommunityControllerTest {

    @Mock
    private AdminCommunityService adminCommunityService;

    @InjectMocks
    private AdminCommunityController adminCommunityController;

    @Test
    void getReportedPostsReturnsPagedSuccessResponse() {
        PageRequest pageable = PageRequest.of(0, 20);
        AdminReportedPostResponse reportedPost =
                new AdminReportedPostResponse(1L, "제목", "내용", 2L, "작성자", 3L, false, LocalDateTime.now());
        when(adminCommunityService.getReportedPosts(pageable))
                .thenReturn(new PageImpl<>(List.of(reportedPost), pageable, 1));

        ResponseEntity<ApiResponse<PageResponse<AdminReportedPostResponse>>> response =
                adminCommunityController.getReportedPosts(pageable);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.ADMIN_REPORTED_POST_LIST_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData().content()).containsExactly(reportedPost);
    }

    @Test
    void mutationEndpointsDelegateAndReturnSuccessResponses() {
        AdminUserSuspendRequest request = new AdminUserSuspendRequest(LocalDateTime.now().plusDays(7));

        assertThat(adminCommunityController.deletePost(1L).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_POST_DELETE_SUCCESS.getHttpStatus());
        assertThat(adminCommunityController.deleteComment(2L).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_COMMENT_DELETE_SUCCESS.getHttpStatus());
        assertThat(adminCommunityController.suspendUser(3L, request).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_USER_SUSPEND_SUCCESS.getHttpStatus());
        assertThat(adminCommunityController.unsuspendUser(3L).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_USER_UNSUSPEND_SUCCESS.getHttpStatus());
        verify(adminCommunityService).deletePost(1L);
        verify(adminCommunityService).deleteComment(2L);
        verify(adminCommunityService).suspendUser(3L, request);
        verify(adminCommunityService).unsuspendUser(3L);
    }
}
