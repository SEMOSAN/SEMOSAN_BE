package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostBlockPolicyTest {

    @Mock
    private UserBlockRepository userBlockRepository;

    @InjectMocks
    private PostBlockPolicy postBlockPolicy;

    @Test
    void findBlockedAuthorIdsDelegatesToRepository() {
        when(userBlockRepository.findBlockedUserIdsByBlocker_Id(1L)).thenReturn(List.of(2L, 3L));

        List<Long> result = postBlockPolicy.findBlockedAuthorIds(1L);

        assertThat(result).containsExactly(2L, 3L);
    }

    @Test
    void validateReadableThrowsWhenViewerBlockedAuthor() throws Exception {
        FreePost post = freePost(10L, user(2L, "author"));

        when(userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> postBlockPolicy.validateReadable(1L, post))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_AUTHOR_BLOCKED);
    }

    private User user(Long id, String oauthId) {
        User user = User.createTestUser(oauthId, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
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
