package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.FreePostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserBlock;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private FreePostRepository freePostRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private UserBlockService userBlockService;

    @Test
    void blockByPostBlocksPostAuthor() throws Exception {
        User blocker = user(1L, "blocker");
        User blockedUser = user(2L, "blocked");
        FreePost post = freePost(10L, blockedUser);

        when(freePostRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(blocker));
        when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(blockedUser));
        when(userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(1L, 2L)).thenReturn(false);
        when(userBlockRepository.saveAndFlush(any(UserBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userBlockService.blockByPost(1L, 10L);

        verify(userBlockRepository).saveAndFlush(any(UserBlock.class));
    }

    @Test
    void blockThrowsWhenBlockingSelf() throws Exception {
        User user = user(1L, "user");
        FreePost post = freePost(10L, user);

        when(freePostRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> userBlockService.blockByPost(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.USER_BLOCK_SELF_NOT_ALLOWED);
        verify(userBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void blockByCommentBlocksCommentAuthor() throws Exception {
        User blocker = user(1L, "blocker");
        User blockedUser = user(2L, "blocked");
        Comment comment = comment(20L, blockedUser);

        when(commentRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(comment));
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(blocker));
        when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(blockedUser));
        when(userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(1L, 2L)).thenReturn(false);
        when(userBlockRepository.saveAndFlush(any(UserBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userBlockService.blockByComment(1L, 20L);

        verify(userBlockRepository).saveAndFlush(any(UserBlock.class));
    }

    @Test
    void blockByCommentThrowsWhenBlockingSelf() throws Exception {
        User user = user(1L, "user");
        Comment comment = comment(20L, user);

        when(commentRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> userBlockService.blockByComment(1L, 20L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.USER_BLOCK_SELF_NOT_ALLOWED);
        verify(userBlockRepository, never()).saveAndFlush(any());
    }

    private User user(Long id, String oauthId) {
        User user = User.createTestUser(oauthId, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", oauthId);
        return user;
    }

    private FreePost freePost(Long id, User author) throws Exception {
        Constructor<FreePost> constructor = FreePost.class.getDeclaredConstructor(User.class, String.class, String.class);
        constructor.setAccessible(true);
        FreePost post = constructor.newInstance(author, "title", "content");
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Comment comment(Long id, User author) throws Exception {
        Comment comment = Comment.create(freePost(10L, author), author, "content");
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
