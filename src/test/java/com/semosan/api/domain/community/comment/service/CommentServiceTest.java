package com.semosan.api.domain.community.comment.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
