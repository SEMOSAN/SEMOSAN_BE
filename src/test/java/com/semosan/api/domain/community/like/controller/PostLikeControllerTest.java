package com.semosan.api.domain.community.like.controller;

import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.like.dto.PostLikeToggleResponse;
import com.semosan.api.domain.community.like.service.PostLikeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostLikeControllerTest {

    @Mock
    private PostLikeService postLikeService;

    @InjectMocks
    private PostLikeController postLikeController;

    @Test
    void toggleReturnsSuccessResponse() {
        PostLikeToggleResponse toggle = new PostLikeToggleResponse(true, 3L);
        when(postLikeService.toggleWithCount(10L, 1L)).thenReturn(toggle);

        assertThat(postLikeController.toggle(1L, 10L).getBody().getData()).isSameAs(toggle);
        verify(postLikeService).toggleWithCount(10L, 1L);
    }

    @Test
    void getCountReturnsSuccessResponse() {
        when(postLikeService.count(10L)).thenReturn(3L);

        var response = postLikeController.getCount(10L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.POST_LIKE_COUNT_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isEqualTo(3L);
    }
}
