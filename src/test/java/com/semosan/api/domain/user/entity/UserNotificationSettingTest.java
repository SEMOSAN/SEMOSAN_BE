package com.semosan.api.domain.user.entity;

import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotificationSettingTest {

    @Test
    void createDefaultInitializesAllSettingsDisabled() {
        User user = User.createTestUser("setting-user", DeviceType.IOS);

        UserNotificationSetting setting = UserNotificationSetting.createDefault(user);

        assertThat(setting.getUser()).isSameAs(user);
        assertThat(setting.isPushNotificationEnabled()).isFalse();
        assertThat(setting.isLiveActivityEnabled()).isFalse();
        assertThat(setting.isVoiceEnabled()).isFalse();
    }

    @Test
    void updatePushNotificationChangesSetting() {
        UserNotificationSetting setting = UserNotificationSetting.createDefault(User.createTestUser("setting-user", DeviceType.IOS));

        setting.updatePushNotification(true);

        assertThat(setting.isPushNotificationEnabled()).isTrue();
    }

    @Test
    void updateLiveActivityChangesSetting() {
        UserNotificationSetting setting = UserNotificationSetting.createDefault(User.createTestUser("setting-user", DeviceType.IOS));

        setting.updateLiveActivity(true);

        assertThat(setting.isLiveActivityEnabled()).isTrue();
    }

    @Test
    void updateVoiceChangesSetting() {
        UserNotificationSetting setting = UserNotificationSetting.createDefault(User.createTestUser("setting-user", DeviceType.IOS));

        setting.updateVoice(true);

        assertThat(setting.isVoiceEnabled()).isTrue();
    }
}
