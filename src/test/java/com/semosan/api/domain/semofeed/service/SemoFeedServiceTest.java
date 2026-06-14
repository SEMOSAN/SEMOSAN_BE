package com.semosan.api.domain.semofeed.service;

import com.semosan.api.domain.semofeed.dto.SemoFeedResponse;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.semofeed.repository.SemoFeedEmojiRepository;
import com.semosan.api.domain.semofeed.repository.SemoFeedRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemoFeedServiceTest {

    @Mock
    private SemoFeedRepository semoFeedRepository;

    @Mock
    private SemoFeedEmojiRepository semoFeedEmojiRepository;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private SemoFeedService semoFeedService;

    @Test
    void createReturnsDefaultEmojiFields() {
        User user = user(1L, "author", "https://example.com/profile.png");

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(semoFeedRepository.save(any(SemoFeed.class))).thenAnswer(invocation -> {
            SemoFeed semoFeed = invocation.getArgument(0);
            ReflectionTestUtils.setField(semoFeed, "id", 10L);
            return semoFeed;
        });

        SemoFeedResponse result = semoFeedService.create(1L, "https://example.com/semofeed.png");

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.mine()).isTrue();
        assertThat(result.emojiCounts())
                .containsEntry(SemoFeedEmojiType.FIRE, 0L)
                .containsEntry(SemoFeedEmojiType.HEART, 0L)
                .containsEntry(SemoFeedEmojiType.CONGRATS, 0L)
                .containsEntry(SemoFeedEmojiType.LAUGH, 0L);
        assertThat(result.reactedByMe())
                .containsEntry(SemoFeedEmojiType.FIRE, false)
                .containsEntry(SemoFeedEmojiType.HEART, false)
                .containsEntry(SemoFeedEmojiType.CONGRATS, false)
                .containsEntry(SemoFeedEmojiType.LAUGH, false);
    }

    @Test
    void listPublicBuildsAuthorEmojiCountsAndReactedByMe() {
        User author = user(1L, "author", "https://example.com/profile.png");
        User me = user(2L, "me", null);
        SemoFeed semoFeed = semoFeed(10L, author);
        PageRequest pageable = PageRequest.of(0, 100);

        when(semoFeedRepository.findPublic(pageable)).thenReturn(new PageImpl<>(List.of(semoFeed), pageable, 1));
        when(semoFeedEmojiRepository.countBySemoFeedIdsGrouped(List.of(10L))).thenReturn(List.of(
                new Object[]{10L, SemoFeedEmojiType.FIRE, 14L},
                new Object[]{10L, SemoFeedEmojiType.LAUGH, 3L}
        ));
        when(semoFeedEmojiRepository.findReactedTypesBySemoFeedIdsAndUserId(List.of(10L), me.getId()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, SemoFeedEmojiType.FIRE}));

        Page<SemoFeedResponse> result = semoFeedService.listPublic(pageable, me.getId());

        SemoFeedResponse response = result.getContent().getFirst();
        assertThat(response.userId()).isEqualTo(author.getId());
        assertThat(response.profileUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(response.nickname()).isEqualTo("author");
        assertThat(response.emojiCounts())
                .containsEntry(SemoFeedEmojiType.FIRE, 14L)
                .containsEntry(SemoFeedEmojiType.HEART, 0L)
                .containsEntry(SemoFeedEmojiType.CONGRATS, 0L)
                .containsEntry(SemoFeedEmojiType.LAUGH, 3L);
        assertThat(response.reactedByMe())
                .containsEntry(SemoFeedEmojiType.FIRE, true)
                .containsEntry(SemoFeedEmojiType.HEART, false)
                .containsEntry(SemoFeedEmojiType.CONGRATS, false)
                .containsEntry(SemoFeedEmojiType.LAUGH, false);
        assertThat(response.mine()).isFalse();

        verify(semoFeedEmojiRepository).countBySemoFeedIdsGrouped(List.of(10L));
        verify(semoFeedEmojiRepository).findReactedTypesBySemoFeedIdsAndUserId(List.of(10L), me.getId());
    }

    @Test
    void listPublicMarksMineWhenFeedAuthorIsMe() {
        User me = user(1L, "me", null);
        SemoFeed semoFeed = semoFeed(10L, me);
        PageRequest pageable = PageRequest.of(0, 100);

        when(semoFeedRepository.findPublic(pageable)).thenReturn(new PageImpl<>(List.of(semoFeed), pageable, 1));
        when(semoFeedEmojiRepository.countBySemoFeedIdsGrouped(List.of(10L))).thenReturn(List.of());
        when(semoFeedEmojiRepository.findReactedTypesBySemoFeedIdsAndUserId(List.of(10L), me.getId()))
                .thenReturn(List.of());

        Page<SemoFeedResponse> result = semoFeedService.listPublic(pageable, me.getId());

        assertThat(result.getContent().getFirst().mine()).isTrue();
    }

    private User user(Long id, String nickname, String profileUrl) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        ReflectionTestUtils.setField(user, "profileUrl", profileUrl);
        return user;
    }

    private SemoFeed semoFeed(Long id, User user) {
        SemoFeed semoFeed = SemoFeed.create(user, "https://example.com/semofeed.png");
        ReflectionTestUtils.setField(semoFeed, "id", id);
        return semoFeed;
    }
}
