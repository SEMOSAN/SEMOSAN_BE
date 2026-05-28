package com.semosan.api.domain.semofeed.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiToggleResponse;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.entity.SemoFeedEmoji;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.semofeed.repository.SemoFeedEmojiRepository;
import com.semosan.api.domain.semofeed.repository.SemoFeedRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemoFeedEmojiServiceTest {

    @Mock
    private SemoFeedEmojiRepository semoFeedEmojiRepository;

    @Mock
    private SemoFeedRepository semoFeedRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SemoFeedEmojiService semoFeedEmojiService;

    @Test
    void toggleWithCountCreatesEmojiReaction() {
        User author = user(1L, "author");
        User reactor = user(2L, "reactor");
        SemoFeed semoFeed = semoFeed(10L, author);

        when(semoFeedRepository.findById(10L)).thenReturn(Optional.of(semoFeed));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reactor));
        when(semoFeedEmojiRepository.findBySemoFeedAndUserAndEmojiType(
                semoFeed,
                reactor,
                SemoFeedEmojiType.FIRE
        )).thenReturn(Optional.empty());
        when(semoFeedEmojiRepository.countBySemoFeedAndEmojiType(semoFeed, SemoFeedEmojiType.FIRE)).thenReturn(1L);

        SemoFeedEmojiToggleResponse result = semoFeedEmojiService.toggleWithCount(
                2L,
                10L,
                SemoFeedEmojiType.FIRE
        );

        assertThat(result.emojiType()).isEqualTo(SemoFeedEmojiType.FIRE);
        assertThat(result.reacted()).isTrue();
        assertThat(result.count()).isEqualTo(1L);
        verify(semoFeedEmojiRepository).save(any(SemoFeedEmoji.class));
    }

    @Test
    void toggleWithCountDeletesExistingEmojiReaction() {
        User author = user(1L, "author");
        User reactor = user(2L, "reactor");
        SemoFeed semoFeed = semoFeed(10L, author);
        SemoFeedEmoji existing = SemoFeedEmoji.create(semoFeed, reactor, SemoFeedEmojiType.LAUGH);

        when(semoFeedRepository.findById(10L)).thenReturn(Optional.of(semoFeed));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reactor));
        when(semoFeedEmojiRepository.findBySemoFeedAndUserAndEmojiType(
                semoFeed,
                reactor,
                SemoFeedEmojiType.LAUGH
        )).thenReturn(Optional.of(existing));
        when(semoFeedEmojiRepository.countBySemoFeedAndEmojiType(semoFeed, SemoFeedEmojiType.LAUGH)).thenReturn(0L);

        SemoFeedEmojiToggleResponse result = semoFeedEmojiService.toggleWithCount(
                2L,
                10L,
                SemoFeedEmojiType.LAUGH
        );

        assertThat(result.reacted()).isFalse();
        assertThat(result.count()).isZero();
        verify(semoFeedEmojiRepository).delete(existing);
    }

    @Test
    void toggleWithCountRejectsOwnSemoFeed() {
        User author = user(1L, "author");
        SemoFeed semoFeed = semoFeed(10L, author);

        when(semoFeedRepository.findById(10L)).thenReturn(Optional.of(semoFeed));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> semoFeedEmojiService.toggleWithCount(1L, 10L, SemoFeedEmojiType.HEART))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.SEMOFEED_EMOJI_SELF_NOT_ALLOWED);
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    private SemoFeed semoFeed(Long id, User user) {
        SemoFeed semoFeed = SemoFeed.create(user, "https://example.com/semofeed.png");
        ReflectionTestUtils.setField(semoFeed, "id", id);
        return semoFeed;
    }
}
