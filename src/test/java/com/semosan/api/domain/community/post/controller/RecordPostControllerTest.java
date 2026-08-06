package com.semosan.api.domain.community.post.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.post.dto.RecordPostCreateRequest;
import com.semosan.api.domain.community.post.dto.RecordPostResponse;
import com.semosan.api.domain.community.post.service.RecordPostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordPostControllerTest {

    @Mock
    private RecordPostService recordPostService;

    @InjectMocks
    private RecordPostController recordPostController;

    @Test
    void createDetailAndDeleteReturnSuccessResponses() {
        RecordPostCreateRequest request = new RecordPostCreateRequest(100L, "내용");
        RecordPostResponse response = response();
        when(recordPostService.create(1L, 100L, "내용")).thenReturn(response);
        when(recordPostService.getDetail(1L, 10L)).thenReturn(response);

        assertThat(recordPostController.create(1L, request).getBody().getData()).isSameAs(response);
        assertThat(recordPostController.getDetail(1L, 10L).getBody().getData()).isSameAs(response);
        assertThat(recordPostController.delete(1L, 10L).getStatusCode())
                .isEqualTo(SuccessStatus.RECORD_POST_DELETE_SUCCESS.getHttpStatus());
        verify(recordPostService).delete(10L, 1L);
    }

    @Test
    void listAndMyListReturnPagedSuccessResponses() {
        PageRequest pageable = PageRequest.of(0, 10);
        RecordPostResponse response = response();
        when(recordPostService.getList(1L, pageable)).thenReturn(new PageImpl<>(List.of(response), pageable, 1));
        when(recordPostService.getMyList(1L, pageable)).thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        assertThat(recordPostController.getList(1L, pageable).getBody().getData().content()).containsExactly(response);
        assertThat(recordPostController.getMyList(1L, pageable).getBody().getData().content()).containsExactly(response);
    }

    private RecordPostResponse response() {
        return new RecordPostResponse(10L, null, "내용", null, 0, LocalDateTime.now());
    }
}
