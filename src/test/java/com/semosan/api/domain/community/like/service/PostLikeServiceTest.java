package com.semosan.api.domain.community.like.service;

import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.like.repository.PostLikeRepository;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PostLikeService postLikeService;

    @Test
    @SuppressWarnings("unchecked")
    void toggleWithCountSendsLikeNotificationWhenLikeCreated() throws Exception {
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(liker));
        when(postLikeRepository.findByPostAndUser(post, liker)).thenReturn(Optional.empty());
        when(postLikeRepository.countByPost(post)).thenReturn(1L);

        postLikeService.toggleWithCount(10L, 2L);

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
    void toggleWithCountDoesNotSendLikeNotificationWhenUnlike() throws Exception {
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        PostLike existing = PostLike.create(post, liker);

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(liker));
        when(postLikeRepository.findByPostAndUser(post, liker)).thenReturn(Optional.of(existing));
        when(postLikeRepository.countByPost(post)).thenReturn(0L);

        postLikeService.toggleWithCount(10L, 2L);

        verify(notificationService, never()).send(any(), any(), any());
    }

    @Test
    void toggleWithCountDoesNotSendLikeNotificationToSelf() throws Exception {
        User author = user(1L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postLikeRepository.findByPostAndUser(post, author)).thenReturn(Optional.empty());
        when(postLikeRepository.countByPost(post)).thenReturn(1L);

        postLikeService.toggleWithCount(10L, 1L);

        verify(notificationService, never()).send(any(), any(), any());
    }

    @Test
    void toggleWithCountDoesNotSendLikeNotificationWhenConcurrentDuplicateDetected() throws Exception {
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(liker));
        when(postLikeRepository.findByPostAndUser(post, liker)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(postLikeRepository.countByPost(post)).thenReturn(1L);

        postLikeService.toggleWithCount(10L, 2L);

        verify(notificationService, never()).send(any(), any(), any());
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
}
