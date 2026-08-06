package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorResponseTest {

    @Test
    void fromReturnsNicknameAndProfileUrlForActiveUser() {
        User user = user(1L, "nickname");
        ReflectionTestUtils.setField(user, "profileUrl", "https://example.com/profile.png");

        AuthorResponse result = AuthorResponse.from(user);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.nickname()).isEqualTo("nickname");
        assertThat(result.profileUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(result.isDeleted()).isFalse();
    }

    @Test
    void fromUsesUnknownNicknameWhenNicknameIsNull() {
        User user = user(1L, "nickname");
        ReflectionTestUtils.setField(user, "nickname", null);

        AuthorResponse result = AuthorResponse.from(user);

        assertThat(result.nickname()).isEqualTo("이름없음");
        assertThat(result.isDeleted()).isFalse();
    }

    @Test
    void fromMasksDeletedUser() {
        User user = user(1L, "nickname");
        ReflectionTestUtils.setField(user, "profileUrl", "https://example.com/profile.png");
        user.withdraw();

        AuthorResponse result = AuthorResponse.from(user);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.nickname()).isEqualTo("탈퇴한 사용자");
        assertThat(result.profileUrl()).isNull();
        assertThat(result.isDeleted()).isTrue();
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }
}
