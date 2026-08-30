package com.semosan.api.domain.community.like.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.like.dto.PostLikeToggleResponse;
import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.like.event.PostLikedEvent;
import com.semosan.api.domain.community.like.repository.PostLikeRepository;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PostLikeService postLikeService;

    @Test
    void toggleWithCountPublishesEventWhenLikeCreated() throws Exception {
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L)).thenReturn(liker);
        when(postLikeRepository.findByPostAndUser(post, liker)).thenReturn(Optional.empty());
        when(postLikeRepository.insertIgnoreConflict(10L, 2L)).thenReturn(1);
        when(postLikeRepository.countByPost(post)).thenReturn(1L);

        PostLikeToggleResponse result = postLikeService.toggleWithCount(10L, 2L);

        assertThat(result.liked()).isTrue();
        assertThat(result.count()).isEqualTo(1L);
        ArgumentCaptor<PostLikedEvent> eventCaptor = ArgumentCaptor.forClass(PostLikedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().postId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().actorId()).isEqualTo(2L);
    }

    @Test
    void toggleWithCountDoesNotPublishEventWhenUnlike() throws Exception {
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
        verify(postLikeRepository).delete(existing);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    // ON CONFLICT DO NOTHING이라 동시 요청이 겹치면 insert row가 0개라 예외 없이 liked=true로 흡수된다.
    @Test
    void toggleWithCountDoesNotPublishEventWhenConcurrentDuplicateDetected() throws Exception {
        User postAuthor = user(1L, "post-author");
        User liker = user(2L, "liker");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L)).thenReturn(liker);
        when(postLikeRepository.findByPostAndUser(post, liker)).thenReturn(Optional.empty());
        when(postLikeRepository.insertIgnoreConflict(10L, 2L)).thenReturn(0);
        when(postLikeRepository.countByPost(post)).thenReturn(1L);

        PostLikeToggleResponse result = postLikeService.toggleWithCount(10L, 2L);

        assertThat(result.liked()).isTrue();
        assertThat(result.count()).isEqualTo(1L);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void countReturnsPostLikeCount() throws Exception {
        User postAuthor = user(1L, "post-author");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.countByPost(post)).thenReturn(7L);

        long result = postLikeService.count(10L);

        assertThat(result).isEqualTo(7L);
    }

    @Test
    void hasLikedReturnsRepositoryResult() throws Exception {
        User postAuthor = user(1L, "post-author");
        User user = user(2L, "user");
        FreePost post = freePost(10L, postAuthor, "제목", "본문");

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L)).thenReturn(user);
        when(postLikeRepository.existsByPostAndUser(post, user)).thenReturn(true);

        boolean result = postLikeService.hasLiked(10L, 2L);

        assertThat(result).isTrue();
    }

    @Test
    void toggleWithCountThrowsWhenPostNotFound() {
        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.toggleWithCount(10L, 2L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_NOT_FOUND);
        verify(userReader, never()).findActiveUserById(2L);
        verify(postLikeRepository, never()).insertIgnoreConflict(any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
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
