package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.response.AdminSemoFeedResponse;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.repository.SemoFeedRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.semosan.api.common.constant.OfficialAccountConstants.SEMOSAN_OFFICIAL_OAUTH_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSemoFeedServiceTest {

    @Mock
    private SemoFeedRepository semoFeedRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminSemoFeedService adminSemoFeedService;

    @Test
    void createUploadsFeedOwnedByOfficialUserAsPublic() {
        when(userRepository.findByOauthIdAndOauthProvider(SEMOSAN_OFFICIAL_OAUTH_ID, OAuthProvider.SYSTEM))
                .thenReturn(Optional.of(officialUser()));
        when(semoFeedRepository.save(any(SemoFeed.class))).thenAnswer(invocation -> {
            SemoFeed semoFeed = invocation.getArgument(0);
            ReflectionTestUtils.setField(semoFeed, "id", 10L);
            return semoFeed;
        });

        AdminSemoFeedResponse response = adminSemoFeedService.create("https://example.com/feed.png");

        assertThat(response.semoFeedId()).isEqualTo(10L);
        assertThat(response.imageUrl()).isEqualTo("https://example.com/feed.png");
        assertThat(response.authorId()).isEqualTo(1L);
        assertThat(response.authorNickname()).isEqualTo("세모산");
        assertThat(response.isPublic()).isTrue();
    }

    @Test
    void createThrowsWhenOfficialUserIsNotSeeded() {
        when(userRepository.findByOauthIdAndOauthProvider(SEMOSAN_OFFICIAL_OAUTH_ID, OAuthProvider.SYSTEM))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminSemoFeedService.create("https://example.com/feed.png"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.USER_NOT_FOUND);

        verify(semoFeedRepository, never()).save(any(SemoFeed.class));
    }

    private User officialUser() {
        User user = User.createTestUser(SEMOSAN_OFFICIAL_OAUTH_ID, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        user.updateNickname("세모산");
        return user;
    }
}
