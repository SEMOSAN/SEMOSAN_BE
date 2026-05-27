package com.semosan.api.domain.community.notification.service;

import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Test
    @SuppressWarnings("unchecked")
    void sendCommentNotificationSendsToPostAuthor() throws Exception {
        CommunityNotificationService service = service();
        User postAuthor = user(1L, "post-author");
        User commentAuthor = user(2L, "comment-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment comment = comment(100L, post, commentAuthor, "댓글입니다");

        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);

        service.sendCommentNotification(post, commentAuthor, comment);

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
    void sendCommentNotificationSkipsSelfAndInactiveReceiver() throws Exception {
        CommunityNotificationService service = service();
        User author = user(1L, "author");
        User inactivePostAuthor = user(2L, "inactive-author");
        FreePost selfPost = freePost(10L, author, "제목", "본문");
        FreePost inactiveAuthorPost = freePost(11L, inactivePostAuthor, "제목", "본문");

        when(userRepository.existsByIdAndDeletedFalse(2L)).thenReturn(false);

        service.sendCommentNotification(selfPost, author, comment(100L, selfPost, author, "내 댓글"));
        service.sendCommentNotification(
                inactiveAuthorPost,
                author,
                comment(101L, inactiveAuthorPost, author, "댓글입니다")
        );

        verify(notificationService, never()).send(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendReplyNotificationSendsToParentAndMentionedUserWithoutDuplicate() throws Exception {
        CommunityNotificationService service = service();
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        User mentionedUser = user(4L, "mentioned-user");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");
        Comment reply = reply(101L, post, replyAuthor, parent, mentionedUser, "대댓글입니다");

        when(userRepository.existsByIdAndDeletedFalse(2L)).thenReturn(true);
        when(userRepository.existsByIdAndDeletedFalse(4L)).thenReturn(true);

        service.sendReplyNotification(post, replyAuthor, parent, mentionedUser, reply);

        ArgumentCaptor<Long> receiverCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService, times(2)).send(
                receiverCaptor.capture(),
                eq(NotificationType.COMMUNITY_REPLY),
                paramsCaptor.capture()
        );
        assertThat(receiverCaptor.getAllValues()).containsExactly(2L, 4L);
        assertThat(paramsCaptor.getAllValues().get(0))
                .containsEntry("actorId", 3L)
                .containsEntry("actorName", "reply-author")
                .containsEntry("postId", 10L)
                .containsEntry("parentCommentId", 100L)
                .containsEntry("commentId", 101L)
                .containsEntry("commentPreview", "대댓글입니다");
    }

    @Test
    void sendReplyNotificationSkipsDuplicateMentionedUser() throws Exception {
        CommunityNotificationService service = service();
        User postAuthor = user(1L, "post-author");
        User parentAuthor = user(2L, "parent-author");
        User replyAuthor = user(3L, "reply-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment parent = comment(100L, post, parentAuthor, "부모 댓글");
        Comment reply = reply(101L, post, replyAuthor, parent, parentAuthor, "대댓글입니다");

        when(userRepository.existsByIdAndDeletedFalse(2L)).thenReturn(true);

        service.sendReplyNotification(post, replyAuthor, parent, parentAuthor, reply);

        verify(notificationService, times(1)).send(eq(2L), eq(NotificationType.COMMUNITY_REPLY), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendPostLikeNotificationSendsToPostAuthor() throws Exception {
        CommunityNotificationService service = service();
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);

        service.sendPostLikeNotification(post, liker);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).send(
                eq(1L),
                eq(NotificationType.COMMUNITY_POST_LIKE),
                paramsCaptor.capture()
        );
        assertThat(paramsCaptor.getValue())
                .containsEntry("actorId", 2L)
                .containsEntry("actorName", "liker")
                .containsEntry("postId", 10L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendCommentNotificationTrimsPreview() throws Exception {
        CommunityNotificationService service = service();
        User postAuthor = user(1L, "post-author");
        User commentAuthor = user(2L, "comment-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        Comment comment = comment(100L, post, commentAuthor, "가".repeat(51));

        when(userRepository.existsByIdAndDeletedFalse(1L)).thenReturn(true);

        service.sendCommentNotification(post, commentAuthor, comment);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).send(
                eq(1L),
                eq(NotificationType.COMMUNITY_COMMENT),
                paramsCaptor.capture()
        );
        assertThat(paramsCaptor.getValue())
                .containsEntry("commentPreview", "가".repeat(50) + "...");
    }

    private CommunityNotificationService service() {
        return new CommunityNotificationService(notificationService, userRepository);
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

    private Comment reply(Long id, FreePost post, User author, Comment parent, User mentionedUser, String content) {
        Comment comment = Comment.reply(post, author, parent, mentionedUser, content);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
