package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.entity.RecordPost;
import com.semosan.api.domain.community.post.repository.RecordPostRepository;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordPostServiceTest {

    @Mock
    private RecordPostRepository recordPostRepository;

    @Mock
    private HikingRecordRepository hikingRecordRepository;

    @Mock
    private HikingMemberRepository hikingMemberRepository;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private RecordPostService recordPostService;

    @Test
    void deleteSoftDeletesWhenRequesterOwnsPost() {
        RecordPost post = recordPost(10L, user(2L, "author"));

        when(recordPostRepository.findById(10L)).thenReturn(Optional.of(post));

        recordPostService.delete(10L, 2L);

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    void deleteThrowsWhenRequesterDoesNotOwnPost() {
        RecordPost post = recordPost(10L, user(2L, "author"));

        when(recordPostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> recordPostService.delete(10L, 3L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_FORBIDDEN);
        assertThat(post.isDeleted()).isFalse();
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    private RecordPost recordPost(Long id, User author) {
        RecordPost post = RecordPost.create(author, "본문", null);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
