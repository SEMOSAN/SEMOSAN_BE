package com.semosan.api.domain.notification.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.enums.DeviceType;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Table(
        name = "fcm_tokens",
        indexes = {@Index(name = "idx_fcm_tokens_user_id", columnList = "user_id")}
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", length = 512, nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", columnDefinition = "device_type_enum", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeviceType deviceType;

    public static FcmToken create(Long userId, String token, DeviceType deviceType) {
        return FcmToken.builder()
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .build();
    }

    // 같은 토큰을 다른 유저가 등록 시 (한 기기 → 다른 유저 로그인) 소유자 갱신
    public void reassignTo(Long userId, DeviceType deviceType) {
        this.userId = userId;
        this.deviceType = deviceType;
    }
}
