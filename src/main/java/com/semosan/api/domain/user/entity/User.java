package com.semosan.api.domain.user.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.enums.DeviceType;
import com.semosan.api.domain.user.enums.Gender;
import com.semosan.api.domain.user.enums.OAuthProvider;
import com.semosan.api.domain.user.enums.OnboardingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 50, nullable = false)
    private String email;

    @Column(name = "name", length = 30)
    private String name;

    @Column(name = "profile_url", length = 255)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", columnDefinition = "device_type_enum", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", columnDefinition = "onboarding_status_enum", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OnboardingStatus onboardingStatus;

    @Column(name = "oauth_id", length = 255, nullable = false)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", columnDefinition = "oauth_provider_enum", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OAuthProvider oauthProvider;

    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", columnDefinition = "gender_enum")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Gender gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

}
