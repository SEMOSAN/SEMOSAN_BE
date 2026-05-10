package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.dto.request.UpdateLiveActivitySettingRequest;
import com.semosan.api.domain.user.dto.request.UpdatePushNotificationSettingRequest;
import com.semosan.api.domain.user.dto.request.UpdateVoiceSettingRequest;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserNotificationSetting;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNotificationSettingService {

    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserRepository userRepository;

    // 로그인한 사용자의 푸시알림 설정을 변경합니다.
    @Transactional
    public void updatePushNotificationSetting(Long userId, UpdatePushNotificationSettingRequest request) {
        findActiveUserById(userId);
        UserNotificationSetting setting = findSetting(userId);
        setting.updatePushNotification(request.enabled());
    }

    // 로그인한 사용자의 라이브 액티비티 설정을 변경합니다.
    @Transactional
    public void updateLiveActivitySetting(Long userId, UpdateLiveActivitySettingRequest request) {
        findActiveUserById(userId);
        UserNotificationSetting setting = findSetting(userId);
        setting.updateLiveActivity(request.enabled());
    }

    // 로그인한 사용자의 음성 설정을 변경합니다.
    @Transactional
    public void updateVoiceSetting(Long userId, UpdateVoiceSettingRequest request) {
        findActiveUserById(userId);
        UserNotificationSetting setting = findSetting(userId);
        setting.updateVoice(request.enabled());
    }

    // userId로 삭제되지 않은 유저를 조회하고, 없으면 예외를 발생시킵니다.
    private User findActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    // 사용자 알림 설정을 조회하고, 없으면 예외를 발생시킵니다.
    private UserNotificationSetting findSetting(Long userId) {
        return userNotificationSettingRepository.findByUser_Id(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_SETTING_NOT_FOUND));
    }
}
