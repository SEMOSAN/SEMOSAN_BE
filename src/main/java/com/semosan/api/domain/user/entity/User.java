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

@Table(name = "users")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 50)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", columnDefinition = "gender_enum")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Gender gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public static User createKakaoUser(
            String oauthId,
            String email,
            String name,
            String profileUrl,
            DeviceType deviceType
    ) {
        return User.builder()
                .oauthId(oauthId)
                .oauthProvider(OAuthProvider.KAKAO)
                .email(email)
                .name(name)
                .profileUrl(profileUrl)
                .deviceType(deviceType)
                .onboardingStatus(OnboardingStatus.INCOMPLETE)
                .deleted(false)
                .build();
    }

    public static User createAppleUser(
            String oauthId,
            String email,
            String name,
            DeviceType deviceType
    ) {
        return User.builder()
                .oauthId(oauthId)
                .oauthProvider(OAuthProvider.APPLE)
                .email(email)
                .name(name)
                .deviceType(deviceType)
                .onboardingStatus(OnboardingStatus.INCOMPLETE)
                .deleted(false)
                .build();
    }

    public static User createTestUser(String testUserId, DeviceType deviceType) {
        return User.builder()
                .oauthId(testUserId)
                .oauthProvider(OAuthProvider.TEST)
                .email(testUserId + "@test.com")
                .name("테스트유저_" + testUserId)
                .deviceType(deviceType)
                .onboardingStatus(OnboardingStatus.INCOMPLETE)
                .deleted(false)
                .build();
    }

    public void restore(String email, String name, String profileUrl, DeviceType deviceType) {
        if (email != null) this.email = email;
        if (name != null) this.name = name;
        if (profileUrl != null) this.profileUrl = profileUrl;
        this.deviceType = deviceType;
        this.onboardingStatus = OnboardingStatus.INCOMPLETE;
        this.deleted = false;
    }

    public void withdraw() {
        this.deleted = true;
    }

}
