package com.semosan.api.domain.user.entity;

import com.semosan.api.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "user_notification_settings")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "push_notification_enabled", nullable = false)
    private boolean pushNotificationEnabled;

    @Column(name = "live_activity_enabled", nullable = false)
    private boolean liveActivityEnabled;

    @Column(name = "voice_enabled", nullable = false)
    private boolean voiceEnabled;

    // 기본값을 적용한 사용자 알림 설정을 생성합니다.
    public static UserNotificationSetting createDefault(User user) {
        return UserNotificationSetting.builder()
                .user(user)
                .pushNotificationEnabled(false)
                .liveActivityEnabled(false)
                .voiceEnabled(false)
                .build();
    }

    // 푸시알림 설정을 변경합니다.
    public void updatePushNotification(boolean enabled) {
        this.pushNotificationEnabled = enabled;
    }

    // 라이브 액티비티 설정을 변경합니다.
    public void updateLiveActivity(boolean enabled) {
        this.liveActivityEnabled = enabled;
    }

    // 음성 설정을 변경합니다.
    public void updateVoice(boolean enabled) {
        this.voiceEnabled = enabled;
    }
}
