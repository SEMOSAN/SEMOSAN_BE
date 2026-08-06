package com.semosan.api.domain.community.post.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.post.dto.FreePostCreateRequest;
import com.semosan.api.domain.community.post.dto.FreePostDetailResponse;
import com.semosan.api.domain.community.post.dto.FreePostListResponse;
import com.semosan.api.domain.community.post.dto.FreePostReportRequest;
import com.semosan.api.domain.community.post.dto.FreePostUpdateRequest;
import com.semosan.api.domain.community.post.enums.FreePostReportReason;
import com.semosan.api.domain.community.post.service.FreePostReportService;
import com.semosan.api.domain.community.post.service.FreePostService;
import com.semosan.api.domain.user.service.UserBlockService;
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
class FreePostControllerTest {

    @Mock
    private FreePostService freePostService;

    @Mock
    private FreePostReportService freePostReportService;

    @Mock
    private UserBlockService userBlockService;

    @InjectMocks
    private FreePostController freePostController;

    @Test
    void createUpdateAndDetailReturnSuccessResponses() {
        FreePostDetailResponse detail = detail();
        FreePostCreateRequest createRequest = new FreePostCreateRequest("제목", "내용", List.of("a.jpg"), 0);
        FreePostUpdateRequest updateRequest = new FreePostUpdateRequest("수정", "수정 내용", List.of("b.jpg"), 0);
        when(freePostService.create(1L, "제목", "내용", List.of("a.jpg"), 0)).thenReturn(detail);
        when(freePostService.getDetail(1L, 10L)).thenReturn(detail);
        when(freePostService.update(1L, 10L, "수정", "수정 내용", List.of("b.jpg"), 0)).thenReturn(detail);

        assertThat(freePostController.create(1L, createRequest).getBody().getData()).isSameAs(detail);
        assertThat(freePostController.getDetail(1L, 10L).getBody().getData()).isSameAs(detail);
        assertThat(freePostController.update(1L, 10L, updateRequest).getBody().getData()).isSameAs(detail);
    }

    @Test
    void listSearchAndMyListReturnPagedSuccessResponses() {
        PageRequest pageable = PageRequest.of(0, 10);
        FreePostListResponse item = listResponse();
        when(freePostService.getList(1L, pageable)).thenReturn(new PageImpl<>(List.of(item), pageable, 1));
        when(freePostService.search(1L, "검색", pageable)).thenReturn(new PageImpl<>(List.of(item), pageable, 1));
        when(freePostService.getMyList(1L, pageable)).thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        assertThat(freePostController.getList(1L, pageable).getBody().getData().content()).containsExactly(item);
        assertThat(freePostController.search(1L, "검색", pageable).getBody().getData().content()).containsExactly(item);
        assertThat(freePostController.getMyList(1L, pageable).getBody().getData().content()).containsExactly(item);
    }

    @Test
    void deleteReportAndBlockDelegateAndReturnSuccessResponses() {
        FreePostReportRequest request = new FreePostReportRequest(FreePostReportReason.SPAM);

        assertThat(freePostController.delete(1L, 10L).getStatusCode())
                .isEqualTo(SuccessStatus.FREE_POST_DELETE_SUCCESS.getHttpStatus());
        assertThat(freePostController.report(1L, 10L, request).getStatusCode())
                .isEqualTo(SuccessStatus.FREE_POST_REPORT_SUCCESS.getHttpStatus());
        assertThat(freePostController.block(1L, 10L).getStatusCode())
                .isEqualTo(SuccessStatus.FREE_POST_BLOCK_SUCCESS.getHttpStatus());
        verify(freePostService).delete(10L, 1L);
        verify(freePostReportService).report(1L, 10L, FreePostReportReason.SPAM);
        verify(userBlockService).blockByPost(1L, 10L);
    }

    private FreePostDetailResponse detail() {
        return new FreePostDetailResponse(10L, null, "제목", "내용", List.of(), 0, 1L, false, 2L, LocalDateTime.now());
    }

    private FreePostListResponse listResponse() {
        return new FreePostListResponse(10L, null, "제목", "내용", "thumb.jpg", 0, 1L, 2L, LocalDateTime.now());
    }
}
