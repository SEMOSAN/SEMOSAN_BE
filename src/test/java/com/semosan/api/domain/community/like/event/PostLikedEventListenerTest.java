package com.semosan.api.domain.community.like.event;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostLikedEventListenerTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private CommunityNotificationService communityNotificationService;

    @InjectMocks
    private PostLikedEventListener listener;

    @Test
    void onPostLikedSendsNotificationWithReloadedPostAndActor() throws Exception {
        User author = user(1L, "author");
        User actor = user(2L, "actor");
        FreePost post = freePost(10L, author);

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L)).thenReturn(actor);

        listener.onPostLiked(new PostLikedEvent(10L, 2L));

        verify(communityNotificationService).sendPostLikeNotification(post, actor);
    }

    @Test
    void onPostLikedSkipsNotificationWhenPostNotFound() {
        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.empty());

        listener.onPostLiked(new PostLikedEvent(10L, 2L));

        verify(userReader, never()).findActiveUserById(2L);
        verify(communityNotificationService, never()).sendPostLikeNotification(any(), any());
    }

    @Test
    void onPostLikedSkipsNotificationWhenActorNotFound() throws Exception {
        User author = user(1L, "author");
        FreePost post = freePost(10L, author);

        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userReader.findActiveUserById(2L))
                .thenThrow(new GeneralException(ErrorStatus.USER_NOT_FOUND));

        assertThatCode(() -> listener.onPostLiked(new PostLikedEvent(10L, 2L)))
                .doesNotThrowAnyException();

        verify(communityNotificationService, never()).sendPostLikeNotification(any(), any());
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    private FreePost freePost(Long id, User author) throws Exception {
        Constructor<FreePost> constructor = FreePost.class.getDeclaredConstructor(User.class, String.class, String.class);
        constructor.setAccessible(true);
        FreePost post = constructor.newInstance(author, "제목", "본문");
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
