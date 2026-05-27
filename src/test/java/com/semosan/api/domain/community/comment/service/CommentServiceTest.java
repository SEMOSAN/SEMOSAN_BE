package com.semosan.api.domain.community.comment.service;

import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    @Test
    @SuppressWarnings("unchecked")
    void createSendsCommentNotificationToPostAuthor() throws Exception {
        User postAuthor = user(1L, "post-author");
        User commentAuthor = user(2L, "comment-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return comment;
        });

        Comment result = commentService.create(10L, 2L, "댓글입니다");

        assertThat(result.getId()).isEqualTo(100L);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).send(
                eq(1L),
                eq(NotificationType.COMMUNITY_COMMENT),
                paramsCaptor.capture()
        );
        assertThat(paramsCaptor.getValue())
                .containsEntry("actorId", 2L)
                .containsEntry("actorName", "comment-author")
                .containsEntry("postId", 10L)
                .containsEntry("commentId", 100L)
                .containsEntry("commentPreview", "댓글입니다");
    }

    @Test
    void createDoesNotSendCommentNotificationToSelf() throws Exception {
        User author = user(1L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.create(10L, 1L, "내 댓글");

        verify(notificationService, never()).send(any(), any(), any());
    }

    @Test
    void createDoesNotSendCommentNotificationToInactivePostAuthor() throws Exception {
        User postAuthor = user(1L, "post-author");
        User commentAuthor = user(2L, "comment-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(false);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return comment;
        });

        Comment result = commentService.create(10L, 2L, "댓글입니다");

        assertThat(result.getId()).isEqualTo(100L);
        verify(notificationService, never()).send(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void replySendsReplyNotificationToParentCommentAuthor() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(3L)).thenReturn(Optional.of(replyAuthor));
        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(parent));
        when(userRepository.existsByIdAndDeletedFalse(2L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 101L);
            return comment;
        });

        Comment result = commentService.reply(10L, 3L, 100L, null, "대댓글입니다");

        assertThat(result.getId()).isEqualTo(101L);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).send(
                eq(2L),
                eq(NotificationType.COMMUNITY_REPLY),
                paramsCaptor.capture()
        );
        assertThat(paramsCaptor.getValue())
                .containsEntry("actorId", 3L)
                .containsEntry("actorName", "reply-author")
                .containsEntry("postId", 10L)
                .containsEntry("parentCommentId", 100L)
                .containsEntry("commentId", 101L)
                .containsEntry("commentPreview", "대댓글입니다");
    }

    @Test
    void replyDoesNotSendReplyNotificationToSelf() throws Exception {
        User postAuthor = user(1L, "post-author");
        User author = user(2L, "author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, author, "내 댓글");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(author));
        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.reply(10L, 2L, 100L, null, "내 대댓글");

        verify(notificationService, never()).send(any(), any(), any());
    }

    @Test
    void replyDoesNotSendReplyNotificationToInactiveParentAuthor() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(3L)).thenReturn(Optional.of(replyAuthor));
        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(parent));
        when(userRepository.existsByIdAndDeletedFalse(2L)).thenReturn(false);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 101L);
            return comment;
        });

        Comment result = commentService.reply(10L, 3L, 100L, null, "대댓글입니다");

        assertThat(result.getId()).isEqualTo(101L);
        verify(notificationService, never()).send(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void replySendsReplyNotificationToMentionedUser() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        User mentionedUser = user(4L, "mentioned-user");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(3L)).thenReturn(Optional.of(replyAuthor));
        when(userRepository.findById(4L)).thenReturn(Optional.of(mentionedUser));
        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(parent));
        when(userRepository.existsByIdAndDeletedFalse(2L)).thenReturn(true);
        when(userRepository.existsByIdAndDeletedFalse(4L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 101L);
            return comment;
        });

        commentService.reply(10L, 3L, 100L, 4L, "대댓글입니다");

        ArgumentCaptor<Long> receiverCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService, times(2)).send(
                receiverCaptor.capture(),
                eq(NotificationType.COMMUNITY_REPLY),
                paramsCaptor.capture()
        );
        assertThat(receiverCaptor.getAllValues()).containsExactly(2L, 4L);
        assertThat(paramsCaptor.getAllValues().get(1))
                .containsEntry("actorId", 3L)
                .containsEntry("actorName", "reply-author")
                .containsEntry("postId", 10L)
                .containsEntry("parentCommentId", 100L)
                .containsEntry("commentId", 101L)
                .containsEntry("commentPreview", "대댓글입니다");
    }

    @Test
    void replyDoesNotSendDuplicateNotificationWhenMentionedUserIsParentAuthor() throws Exception {
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(3L)).thenReturn(Optional.of(replyAuthor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(parentAuthor));
        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(parent));
        when(userRepository.existsByIdAndDeletedFalse(2L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 101L);
            return comment;
        });

        commentService.reply(10L, 3L, 100L, 2L, "대댓글입니다");

        verify(notificationService, times(1)).send(
                eq(2L),
                eq(NotificationType.COMMUNITY_REPLY),
                any()
        );
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
}
