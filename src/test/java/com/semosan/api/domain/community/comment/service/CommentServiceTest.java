package com.semosan.api.domain.community.comment.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.comment.dto.CommentResponse;
import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.notification.service.CommunityNotificationService;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private CommunityNotificationService communityNotificationService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createSavesCommentAndDelegatesNotification() throws Exception {
        User postAuthor = user(1L, "post-author");
        User commentAuthor = user(2L, "comment-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L)).thenReturn(commentAuthor);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return comment;
        });

        Comment result = commentService.create(10L, 2L, "댓글입니다");

        assertThat(result.getId()).isEqualTo(100L);
        verify(communityNotificationService).sendCommentNotification(post, commentAuthor, result);
    }

    @Test
    void replySavesReplyAndDelegatesNotification() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        User mentionedUser = user(4L, "mentioned-user");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(3L)).thenReturn(replyAuthor);
        when(userReader.findActiveUserById(4L)).thenReturn(mentionedUser);
        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 101L);
            return comment;
        });

        Comment result = commentService.reply(10L, 3L, 100L, 4L, "대댓글입니다");

        assertThat(result.getId()).isEqualTo(101L);
        verify(communityNotificationService)
                .sendReplyNotification(post, replyAuthor, parent, mentionedUser, result);
    }

    @Test
    void replyNormalizesNestedReplyParentAndAllowsNoMentionedUser() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User nestedAuthor = user(3L, "nested-author");
        User replyAuthor = user(4L, "reply-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");
        Comment nestedReply = reply(101L, post, nestedAuthor, parent, "기존 대댓글");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(4L)).thenReturn(replyAuthor);
        when(commentRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(nestedReply));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 102L);
            return comment;
        });

        Comment result = commentService.reply(10L, 4L, 101L, null, "새 대댓글");

        assertThat(result.getParent()).isSameAs(parent);
        assertThat(result.getMentionedUser()).isNull();
        verify(userReader, never()).findActiveUserById(null);
        verify(communityNotificationService)
                .sendReplyNotification(post, replyAuthor, parent, null, result);
    }

    @Test
    void replyThrowsWhenParentBelongsToDifferentPost() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        FreePost requestedPost = freePost(10L, postAuthor, "제목", "본문");
        FreePost otherPost = freePost(11L, postAuthor, "다른 제목", "다른 본문");
        Comment parent = comment(100L, otherPost, parentAuthor, "부모 댓글");

        when(postRepository.findById(10L)).thenReturn(Optional.of(requestedPost));
        when(userReader.findActiveUserById(3L)).thenReturn(replyAuthor);
        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.reply(10L, 3L, 100L, null, "대댓글"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COMMENT_PARENT_POST_MISMATCH);
        verify(commentRepository, never()).save(any(Comment.class));
        verify(communityNotificationService, never()).sendReplyNotification(any(), any(), any(), any(), any());
    }

    @Test
    void createThrowsWhenPostNotFound() {
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(10L, 2L, "댓글"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_NOT_FOUND);
        verify(userReader, never()).findActiveUserById(2L);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void getCommentsByPostMapsVisibleParentsWithBlockedUsers() throws Exception {
        User postAuthor = user(1L, "post-author");
        User normalAuthor = user(2L, "normal-author");
        User blockedAuthor = user(3L, "blocked-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment normalComment = comment(100L, post, normalAuthor, "일반 댓글");
        Comment blockedComment = comment(101L, post, blockedAuthor, "차단 댓글");
        PageRequest pageable = PageRequest.of(0, 10);

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userBlockRepository.findBlockedUserIdsByBlocker_Id(9L)).thenReturn(List.of(3L));
        when(commentRepository.findVisibleParentsByPost(post, pageable))
                .thenReturn(new PageImpl<>(List.of(normalComment, blockedComment), pageable, 2));

        Page<CommentResponse> result = commentService.getCommentsByPost(10L, 9L, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).content()).isEqualTo("일반 댓글");
        assertThat(result.getContent().get(0).isBlocked()).isFalse();
        assertThat(result.getContent().get(1).content()).isEqualTo("차단한 사용자입니다.");
        assertThat(result.getContent().get(1).isBlocked()).isTrue();
    }

    @Test
    void deleteSoftDeletesWhenRequesterOwnsComment() throws Exception {
        User postAuthor = user(1L, "post-author");
        User commentAuthor = user(2L, "comment-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment comment = comment(100L, post, commentAuthor, "댓글");

        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(comment));

        commentService.delete(100L, 2L);

        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    void deleteThrowsWhenRequesterDoesNotOwnComment() throws Exception {
        User postAuthor = user(1L, "post-author");
        User commentAuthor = user(2L, "comment-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment comment = comment(100L, post, commentAuthor, "댓글");

        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(100L, 3L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COMMENT_FORBIDDEN);
        assertThat(comment.isDeleted()).isFalse();
    }

    @Test
    void getRepliesMasksBlockedAuthors() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User blockedAuthor = user(3L, "blocked-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");
        Comment reply = reply(101L, post, blockedAuthor, parent, "차단 유저 대댓글");

        when(commentRepository.findById(100L)).thenReturn(Optional.of(parent));
        when(userBlockRepository.findBlockedUserIdsByBlocker_Id(9L)).thenReturn(List.of(3L));
        when(commentRepository.findByParentAndDeletedFalseOrderByCreatedAtAsc(parent)).thenReturn(List.of(reply));

        List<CommentResponse> result = commentService.getReplies(100L, 9L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isBlocked()).isTrue();
        assertThat(result.get(0).content()).isEqualTo("차단한 사용자입니다.");
        assertThat(result.get(0).author().id()).isEqualTo(3L);
    }

    @Test
    void getRepliesReturnsFullContentForNonBlockedAuthor() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");
        Comment reply = reply(101L, post, replyAuthor, parent, "일반 대댓글");

        when(commentRepository.findById(100L)).thenReturn(Optional.of(parent));
        when(userBlockRepository.findBlockedUserIdsByBlocker_Id(9L)).thenReturn(List.of(4L));
        when(commentRepository.findByParentAndDeletedFalseOrderByCreatedAtAsc(parent)).thenReturn(List.of(reply));

        List<CommentResponse> result = commentService.getReplies(100L, 9L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isBlocked()).isFalse();
        assertThat(result.get(0).content()).isEqualTo("일반 대댓글");
        assertThat(result.get(0).author().id()).isEqualTo(3L);
    }

    @Test
    void getRepliesThrowsWhenParentNotFound() {
        when(commentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getReplies(100L, 9L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COMMENT_NOT_FOUND);
        verify(userBlockRepository, never()).findBlockedUserIdsByBlocker_Id(9L);
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    private FreePost freePost(Long id, User author, String title, String content) throws Exception {
        Constructor<FreePost> constructor = FreePost.class.getDeclaredConstructor(User.class, String.class, String.class);
        constructor.setAccessible(true);
        FreePost post = constructor.newInstance(author, title, content);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Comment comment(Long id, FreePost post, User author, String content) {
        Comment comment = Comment.create(post, author, content);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private Comment reply(Long id, FreePost post, User author, Comment parent, String content) {
        Comment comment = Comment.reply(post, author, parent, null, content);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
