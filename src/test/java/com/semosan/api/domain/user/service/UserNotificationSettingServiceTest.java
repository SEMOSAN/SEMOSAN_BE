package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.dto.request.UpdateNotificationSettingRequest;
import com.semosan.api.domain.user.dto.response.GetNotificationSettingResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserNotificationSetting;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationSettingServiceTest {

    @Mock
    private UserNotificationSettingRepository userNotificationSettingRepository;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private UserNotificationSettingService service;

    @Test
    void updatePushNotificationSettingUpdatesSetting() {
        UserNotificationSetting setting = setting();
        when(userNotificationSettingRepository.findByUser_Id(1L)).thenReturn(Optional.of(setting));

        service.updatePushNotificationSetting(1L, new UpdateNotificationSettingRequest(true));

        verify(userReader).findActiveUserById(1L);
        assertThat(setting.isPushNotificationEnabled()).isTrue();
    }

    @Test
    void updateLiveActivitySettingUpdatesSetting() {
        UserNotificationSetting setting = setting();
        when(userNotificationSettingRepository.findByUser_Id(1L)).thenReturn(Optional.of(setting));

        service.updateLiveActivitySetting(1L, new UpdateNotificationSettingRequest(true));

        assertThat(setting.isLiveActivityEnabled()).isTrue();
    }

    @Test
    void updateVoiceSettingUpdatesSetting() {
        UserNotificationSetting setting = setting();
        when(userNotificationSettingRepository.findByUser_Id(1L)).thenReturn(Optional.of(setting));

        service.updateVoiceSetting(1L, new UpdateNotificationSettingRequest(true));

        assertThat(setting.isVoiceEnabled()).isTrue();
    }

    @Test
    void getNotificationSettingReturnsCurrentValues() {
        UserNotificationSetting setting = setting();
        setting.updatePushNotification(true);
        setting.updateLiveActivity(true);
        when(userNotificationSettingRepository.findByUser_Id(1L)).thenReturn(Optional.of(setting));

        GetNotificationSettingResponse response = service.getNotificationSetting(1L);

        verify(userReader).findActiveUserById(1L);
        assertThat(response.pushNotificationEnabled()).isTrue();
        assertThat(response.liveActivityEnabled()).isTrue();
        assertThat(response.voiceEnabled()).isFalse();
    }

    @Test
    void updateThrowsWhenSettingDoesNotExist() {
        when(userNotificationSettingRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateVoiceSetting(1L, new UpdateNotificationSettingRequest(true)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_SETTING_NOT_FOUND);
    }

    private UserNotificationSetting setting() {
        return UserNotificationSetting.createDefault(User.createTestUser("test-user", DeviceType.IOS));
    }
}
