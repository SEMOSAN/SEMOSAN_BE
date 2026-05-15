package com.semosan.api.domain.user.dto.response;

import com.semosan.api.domain.user.entity.UserNotificationSetting;

public record GetNotificationSettingResponse(
        boolean pushNotificationEnabled,
        boolean liveActivityEnabled,
        boolean voiceEnabled
) {
    // 사용자 알림 설정으로 응답 DTO를 생성합니다.
    public static GetNotificationSettingResponse from(UserNotificationSetting setting) {
        return new GetNotificationSettingResponse(
                setting.isPushNotificationEnabled(),
                setting.isLiveActivityEnabled(),
                setting.isVoiceEnabled()
        );
    }
}
