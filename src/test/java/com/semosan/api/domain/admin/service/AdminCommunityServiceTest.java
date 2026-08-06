package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminUserSuspendRequest;
import com.semosan.api.domain.admin.dto.response.AdminReportedPostResponse;
import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.repository.FreePostReportRepository;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.community.post.repository.ReportedPostProjection;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCommunityServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FreePostReportRepository freePostReportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminCommunityService adminCommunityService;

    @Test
    void getReportedPostsMapsProjectionToResponse() {
        ReportedPostProjection projection = mock(ReportedPostProjection.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 6, 13, 0);
        when(projection.getPostId()).thenReturn(1L);
        when(projection.getTitle()).thenReturn("신고 게시글");
        when(projection.getContent()).thenReturn("내용");
        when(projection.getAuthorId()).thenReturn(2L);
        when(projection.getAuthorNickname()).thenReturn("작성자");
        when(projection.getReportCount()).thenReturn(3L);
        when(projection.getDeleted()).thenReturn(false);
        when(projection.getCreatedAt()).thenReturn(createdAt);
        PageRequest pageable = PageRequest.of(0, 10);
        when(freePostReportRepository.findReportedPosts(pageable))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

        Page<AdminReportedPostResponse> result = adminCommunityService.getReportedPosts(pageable);

        AdminReportedPostResponse response = result.getContent().getFirst();
        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("신고 게시글");
        assertThat(response.content()).isEqualTo("내용");
        assertThat(response.authorId()).isEqualTo(2L);
        assertThat(response.authorNickname()).isEqualTo("작성자");
        assertThat(response.reportCount()).isEqualTo(3L);
        assertThat(response.deleted()).isFalse();
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void deletePostSoftDeletesPost() {
        Post post = FreePost.create(user(1L), "제목", "내용");
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        adminCommunityService.deletePost(10L);

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    void deletePostThrowsWhenPostMissing() {
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.deletePost(10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_NOT_FOUND);
    }

    @Test
    void deleteCommentSoftDeletesComment() {
        Post post = FreePost.create(user(1L), "제목", "내용");
        Comment comment = Comment.create(post, user(2L), "댓글");
        when(commentRepository.findById(20L)).thenReturn(Optional.of(comment));

        adminCommunityService.deleteComment(20L);

        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    void deleteCommentThrowsWhenCommentMissing() {
        when(commentRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.deleteComment(20L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COMMENT_NOT_FOUND);
    }

    @Test
    void suspendUserStoresSuspendedUntil() {
        User user = user(1L);
        LocalDateTime suspendedUntil = LocalDateTime.now().plusDays(7);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminCommunityService.suspendUser(1L, new AdminUserSuspendRequest(suspendedUntil));

        assertThat(user.getSuspendedUntil()).isEqualTo(suspendedUntil);
        assertThat(user.isSuspended()).isTrue();
    }

    @Test
    void suspendUserThrowsWhenUserMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.suspendUser(1L,
                new AdminUserSuspendRequest(LocalDateTime.now().plusDays(1))))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.USER_NOT_FOUND);
    }

    @Test
    void unsuspendUserClearsSuspendedUntil() {
        User user = user(1L);
        user.suspend(LocalDateTime.now().plusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminCommunityService.unsuspendUser(1L);

        assertThat(user.getSuspendedUntil()).isNull();
        assertThat(user.isSuspended()).isFalse();
    }

    @Test
    void unsuspendUserThrowsWhenUserMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.unsuspendUser(1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.USER_NOT_FOUND);
    }

    private User user(Long id) {
        User user = User.createTestUser("test-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
