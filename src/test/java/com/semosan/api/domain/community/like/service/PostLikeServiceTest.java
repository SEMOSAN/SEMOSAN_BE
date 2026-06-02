package com.semosan.api.domain.community.like.service;

import com.semosan.api.domain.community.like.dto.PostLikeToggleResponse;
import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.like.repository.PostLikeRepository;
import com.semosan.api.domain.community.notification.service.CommunityNotificationService;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private UserReader userReader;

    @Mock
    private CommunityNotificationService communityNotificationService;

    @InjectMocks
    private PostLikeService postLikeService;

    @Test
    void toggleWithCountDelegatesNotificationWhenLikeCreated() throws Exception {
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L)).thenReturn(liker);
        when(postLikeRepository.findByPostAndUser(post, liker)).thenReturn(Optional.empty());
        when(postLikeRepository.countByPost(post)).thenReturn(1L);

        PostLikeToggleResponse result = postLikeService.toggleWithCount(10L, 2L);

        assertThat(result.liked()).isTrue();
        assertThat(result.count()).isEqualTo(1L);
        verify(communityNotificationService).sendPostLikeNotification(post, liker);
    }

    @Test
    void toggleWithCountDoesNotDelegateNotificationWhenUnlike() throws Exception {
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");
        PostLike existing = PostLike.create(post, liker);

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L)).thenReturn(liker);
        when(postLikeRepository.findByPostAndUser(post, liker)).thenReturn(Optional.of(existing));
        when(postLikeRepository.countByPost(post)).thenReturn(0L);

        PostLikeToggleResponse result = postLikeService.toggleWithCount(10L, 2L);

        assertThat(result.liked()).isFalse();
        assertThat(result.count()).isZero();
        verify(communityNotificationService, never()).sendPostLikeNotification(any(), any());
    }

    @Test
    void toggleWithCountDoesNotDelegateNotificationWhenConcurrentDuplicateDetected() throws Exception {
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L)).thenReturn(liker);
        when(postLikeRepository.findByPostAndUser(post, liker)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(postLikeRepository.countByPost(post)).thenReturn(1L);

        PostLikeToggleResponse result = postLikeService.toggleWithCount(10L, 2L);

        assertThat(result.liked()).isTrue();
        assertThat(result.count()).isEqualTo(1L);
        verify(communityNotificationService, never()).sendPostLikeNotification(any(), any());
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
