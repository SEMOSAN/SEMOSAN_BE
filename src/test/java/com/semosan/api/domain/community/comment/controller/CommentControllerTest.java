package com.semosan.api.domain.community.comment.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.comment.dto.CommentCreateRequest;
import com.semosan.api.domain.community.comment.dto.CommentReplyRequest;
import com.semosan.api.domain.community.comment.dto.CommentResponse;
import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.service.CommentService;
import com.semosan.api.domain.user.entity.User;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private UserBlockService userBlockService;

    @InjectMocks
    private CommentController commentController;

    @Test
    void createAndReplyReturnSuccessResponses() {
        Comment comment = comment(10L);
        CommentCreateRequest createRequest = new CommentCreateRequest("댓글");
        CommentReplyRequest replyRequest = new CommentReplyRequest(9L, 2L, "답글");
        when(commentService.create(1L, 2L, "댓글")).thenReturn(comment);
        when(commentService.reply(1L, 2L, 9L, 2L, "답글")).thenReturn(comment);

        assertThat(commentController.create(2L, 1L, createRequest).getStatusCode())
                .isEqualTo(SuccessStatus.COMMENT_CREATE_SUCCESS.getHttpStatus());
        assertThat(commentController.reply(2L, 1L, replyRequest).getStatusCode())
                .isEqualTo(SuccessStatus.COMMENT_REPLY_SUCCESS.getHttpStatus());
    }

    @Test
    void listEndpointsReturnSuccessResponses() {
        PageRequest pageable = PageRequest.of(0, 20);
        CommentResponse response = new CommentResponse(1L, null, "댓글", null, null, LocalDateTime.now(), false, false);
        when(commentService.getCommentsByPost(1L, 2L, pageable))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));
        when(commentService.getReplies(10L, 2L)).thenReturn(List.of(response));

        ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> comments =
                commentController.getComments(2L, 1L, pageable);
        ResponseEntity<ApiResponse<List<CommentResponse>>> replies =
                commentController.getReplies(2L, 10L);

        assertThat(comments.getStatusCode()).isEqualTo(SuccessStatus.COMMENT_LIST_SUCCESS.getHttpStatus());
        assertThat(comments.getBody().getData().content()).containsExactly(response);
        assertThat(replies.getStatusCode()).isEqualTo(SuccessStatus.COMMENT_REPLY_LIST_SUCCESS.getHttpStatus());
        assertThat(replies.getBody().getData()).containsExactly(response);
    }

    @Test
    void deleteAndBlockDelegateAndReturnSuccessResponses() {
        assertThat(commentController.delete(2L, 10L).getStatusCode())
                .isEqualTo(SuccessStatus.COMMENT_DELETE_SUCCESS.getHttpStatus());
        assertThat(commentController.block(2L, 10L).getStatusCode())
                .isEqualTo(SuccessStatus.COMMENT_BLOCK_SUCCESS.getHttpStatus());
        verify(commentService).delete(10L, 2L);
        verify(userBlockService).blockByComment(2L, 10L);
    }

    private Comment comment(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(2L);
        when(user.getNickname()).thenReturn("작성자");
        when(user.getProfileUrl()).thenReturn("profile.jpg");
        Comment comment = mock(Comment.class);
        when(comment.getId()).thenReturn(id);
        when(comment.getAuthor()).thenReturn(user);
        when(comment.getContent()).thenReturn("댓글");
        when(comment.getCreatedAt()).thenReturn(LocalDateTime.now());
        return comment;
    }
}
