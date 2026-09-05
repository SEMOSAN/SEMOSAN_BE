package com.semosan.api.domain.semofeed.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Test
    void listMineBuildsResponsesWithoutReactedLookupWhenFeedIdsAreEmpty() {
        when(semoFeedRepository.findByUserId(1L)).thenReturn(List.of());

        List<SemoFeedResponse> result = semoFeedService.listMine(1L);

        assertThat(result).isEmpty();
        verify(semoFeedEmojiRepository, never()).countBySemoFeedIdsGrouped(any());
        verify(semoFeedEmojiRepository, never()).findReactedTypesBySemoFeedIdsAndUserId(any(), any());
    }

    @Test
    void listMineBuildsEmojiCountsAndMineFlag() {
        User me = user(1L, "me", null);
        SemoFeed semoFeed = semoFeed(10L, me);

        when(semoFeedRepository.findByUserId(1L)).thenReturn(List.of(semoFeed));
        when(semoFeedEmojiRepository.countBySemoFeedIdsGrouped(List.of(10L))).thenReturn(List.<Object[]>of(
                new Object[]{10L, SemoFeedEmojiType.HEART, 2L}
        ));
        when(semoFeedEmojiRepository.findReactedTypesBySemoFeedIdsAndUserId(List.of(10L), 1L))
                .thenReturn(List.<Object[]>of(new Object[]{10L, SemoFeedEmojiType.HEART}));

        List<SemoFeedResponse> result = semoFeedService.listMine(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().mine()).isTrue();
        assertThat(result.getFirst().emojiCounts()).containsEntry(SemoFeedEmojiType.HEART, 2L);
        assertThat(result.getFirst().reactedByMe()).containsEntry(SemoFeedEmojiType.HEART, true);
    }

    @Test
    void listPublicSkipsReactedLookupWhenUserIdIsNull() {
        User author = user(1L, "author", null);
        SemoFeed semoFeed = semoFeed(10L, author);
        PageRequest pageable = PageRequest.of(0, 100);

        when(semoFeedRepository.findPublic(pageable)).thenReturn(new PageImpl<>(List.of(semoFeed), pageable, 1));
        when(semoFeedEmojiRepository.countBySemoFeedIdsGrouped(List.of(10L))).thenReturn(List.of());

        Page<SemoFeedResponse> result = semoFeedService.listPublic(pageable, null);

        assertThat(result.getContent().getFirst().mine()).isFalse();
        assertThat(result.getContent().getFirst().reactedByMe())
                .containsEntry(SemoFeedEmojiType.FIRE, false);
        verify(semoFeedEmojiRepository, never()).findReactedTypesBySemoFeedIdsAndUserId(any(), any());
    }

    @Test
    void togglePublicTogglesOwnedFeed() {
        User owner = user(1L, "owner", null);
        SemoFeed semoFeed = semoFeed(10L, owner);

        when(semoFeedRepository.findById(10L)).thenReturn(Optional.of(semoFeed));

        boolean first = semoFeedService.togglePublic(1L, 10L);
        boolean second = semoFeedService.togglePublic(1L, 10L);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void togglePublicThrowsWhenFeedNotFound() {
        when(semoFeedRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semoFeedService.togglePublic(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.SEMOFEED_NOT_FOUND);
    }

    @Test
    void deleteThrowsWhenRequesterDoesNotOwnFeed() {
        SemoFeed semoFeed = semoFeed(10L, user(2L, "owner", null));

        when(semoFeedRepository.findById(10L)).thenReturn(Optional.of(semoFeed));

        assertThatThrownBy(() -> semoFeedService.delete(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.SEMOFEED_FORBIDDEN);
        verify(semoFeedRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesOwnedFeed() {
        SemoFeed semoFeed = semoFeed(10L, user(1L, "owner", null));

        when(semoFeedRepository.findById(10L)).thenReturn(Optional.of(semoFeed));

        semoFeedService.delete(1L, 10L);

        verify(semoFeedRepository).delete(semoFeed);
    }

    @Test
    void getReturnsPublicFeedToAnyViewer() {
        User author = user(1L, "author", "https://example.com/profile.png");
        SemoFeed semoFeed = semoFeed(10L, author);
        semoFeed.updatePublic(true);

        when(semoFeedRepository.findByIdWithUser(10L)).thenReturn(Optional.of(semoFeed));
        when(semoFeedEmojiRepository.countBySemoFeedIdsGrouped(List.of(10L))).thenReturn(List.<Object[]>of(
                new Object[]{10L, SemoFeedEmojiType.FIRE, 5L}
        ));
        when(semoFeedEmojiRepository.findReactedTypesBySemoFeedIdsAndUserId(List.of(10L), 2L))
                .thenReturn(List.<Object[]>of(new Object[]{10L, SemoFeedEmojiType.FIRE}));

        SemoFeedResponse result = semoFeedService.get(2L, 10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.mine()).isFalse();
        assertThat(result.emojiCounts()).containsEntry(SemoFeedEmojiType.FIRE, 5L);
        assertThat(result.reactedByMe()).containsEntry(SemoFeedEmojiType.FIRE, true);
    }

    @Test
    void getReturnsPrivateFeedToItsAuthor() {
        // 이모지 알림 수신자는 항상 작성자 본인이라 비공개 피드도 딥링크로 열려야 한다.
        User owner = user(1L, "owner", null);
        SemoFeed semoFeed = semoFeed(10L, owner);

        when(semoFeedRepository.findByIdWithUser(10L)).thenReturn(Optional.of(semoFeed));
        when(semoFeedEmojiRepository.countBySemoFeedIdsGrouped(List.of(10L))).thenReturn(List.of());
        when(semoFeedEmojiRepository.findReactedTypesBySemoFeedIdsAndUserId(List.of(10L), 1L))
                .thenReturn(List.of());

        SemoFeedResponse result = semoFeedService.get(1L, 10L);

        assertThat(result.isPublic()).isFalse();
        assertThat(result.mine()).isTrue();
    }

    @Test
    void getThrowsWhenPrivateFeedIsRequestedByAnotherUser() {
        SemoFeed semoFeed = semoFeed(10L, user(1L, "owner", null));

        when(semoFeedRepository.findByIdWithUser(10L)).thenReturn(Optional.of(semoFeed));

        assertThatThrownBy(() -> semoFeedService.get(2L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.SEMOFEED_FORBIDDEN);
    }

    @Test
    void getThrowsWhenFeedNotFound() {
        when(semoFeedRepository.findByIdWithUser(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semoFeedService.get(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.SEMOFEED_NOT_FOUND);
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
