package com.semosan.api.domain.user.entity;

import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserBlockTest {

    @Test
    void createInitializesBlockerAndBlockedUser() {
        User blocker = User.createTestUser("blocker", DeviceType.IOS);
        User blockedUser = User.createTestUser("blocked", DeviceType.ANDROID);

        UserBlock userBlock = UserBlock.create(blocker, blockedUser);

        assertThat(userBlock.getBlocker()).isSameAs(blocker);
        assertThat(userBlock.getBlockedUser()).isSameAs(blockedUser);
    }
}
