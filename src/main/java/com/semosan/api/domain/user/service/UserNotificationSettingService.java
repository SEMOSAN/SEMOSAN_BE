package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.dto.request.UpdateNotificationSettingRequest;
import com.semosan.api.domain.user.dto.response.GetNotificationSettingResponse;
import com.semosan.api.domain.user.entity.UserNotificationSetting;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class UserNotificationSettingService {

    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserReader userReader;

    // 로그인한 사용자의 푸시알림 설정을 변경합니다.
    @Transactional
    public void updatePushNotificationSetting(Long userId, UpdateNotificationSettingRequest request) {
        updateSetting(userId, setting -> setting.updatePushNotification(request.enabled()));
    }

    // 로그인한 사용자의 라이브 액티비티 설정을 변경합니다.
    @Transactional
    public void updateLiveActivitySetting(Long userId, UpdateNotificationSettingRequest request) {
        updateSetting(userId, setting -> setting.updateLiveActivity(request.enabled()));
    }

    // 로그인한 사용자의 음성 설정을 변경합니다.
    @Transactional
    public void updateVoiceSetting(Long userId, UpdateNotificationSettingRequest request) {
        updateSetting(userId, setting -> setting.updateVoice(request.enabled()));
    }

    // 로그인한 사용자의 알림 설정을 조회합니다.
    @Transactional(readOnly = true)
    public GetNotificationSettingResponse getNotificationSetting(Long userId) {
        userReader.findCompletedOnboardingUserById(userId);
        return GetNotificationSettingResponse.from(findSetting(userId));
    }

    // 사용자 알림 설정을 조회한 뒤 전달받은 변경 동작을 적용합니다.
    private void updateSetting(Long userId, Consumer<UserNotificationSetting> updater) {
        userReader.findCompletedOnboardingUserById(userId);
        UserNotificationSetting setting = findSetting(userId);
        updater.accept(setting);
    }

    // 사용자 알림 설정을 조회하고, 없으면 예외를 발생시킵니다.
    private UserNotificationSetting findSetting(Long userId) {
        return userNotificationSettingRepository.findByUser_Id(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_SETTING_NOT_FOUND));
    }
}
