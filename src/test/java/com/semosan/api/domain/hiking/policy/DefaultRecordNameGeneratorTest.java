package com.semosan.api.domain.hiking.policy;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRecordNameGeneratorTest {

    private static final LocalDateTime HIKED_AT = LocalDateTime.of(2026, 7, 23, 9, 30);

    private final DefaultRecordNameGenerator generator = new DefaultRecordNameGenerator();

    @Test
    void generatesNameFromDateNicknameAndSequence() {
        assertThat(generator.generate(userWithNickname("등산왕"), HIKED_AT, 1))
                .isEqualTo("260723_등산왕의코스1");
    }

    @Test
    void usesSequenceToDistinguishSameDayRecords() {
        User user = userWithNickname("등산왕");

        assertThat(generator.generate(user, HIKED_AT, 2)).isEqualTo("260723_등산왕의코스2");
        assertThat(generator.generate(user, HIKED_AT, 3)).isEqualTo("260723_등산왕의코스3");
    }

    @Test
    void fallsBackToRealNameWhenNicknameIsMissing() {
        User user = userWithNickname(null);
        ReflectionTestUtils.setField(user, "name", "장인호");

        assertThat(generator.generate(user, HIKED_AT, 1)).isEqualTo("260723_장인호의코스1");
    }

    @Test
    void fallsBackToPlaceholderWhenNicknameAndNameAreMissing() {
        // 탈퇴 처리된 유저는 닉네임/실명이 모두 null 로 밀린다.
        User user = userWithNickname(null);
        ReflectionTestUtils.setField(user, "name", null);

        assertThat(generator.generate(user, HIKED_AT, 1)).isEqualTo("260723_사용자의코스1");
    }

    @Test
    void truncatesDisplayNameButKeepsDateAndSequence() {
        // 실명 fallback 은 길이 상한이 없다. 잘라내더라도 구분에 필요한 날짜/순번은 살아야 한다.
        User user = userWithNickname(null);
        ReflectionTestUtils.setField(user, "name", "가".repeat(200));

        String name = generator.generate(user, HIKED_AT, 7);

        assertThat(name).hasSizeLessThanOrEqualTo(100);
        assertThat(name).startsWith("260723_");
        assertThat(name).endsWith("의코스7");
    }

    private User userWithNickname(String nickname) {
        User user = User.createTestUser("record-name-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }
}
