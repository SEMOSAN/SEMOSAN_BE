package com.semosan.api.domain.user.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand;
import com.semosan.api.domain.user.dto.command.UpdateUserProfileCommand;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.Gender;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.Period;

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

    @Column(name = "nickname", length = 30)
    private String nickname;

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

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "height")
    private Double height;

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

    public void withdraw() {
        this.email = null;
        this.name = null;
        this.nickname = null;
        this.profileUrl = null;
        this.gender = null;
        this.age = null;
        this.birthDate = null;
        this.height = null;
        this.weight = null;
        this.onboardingStatus = OnboardingStatus.INCOMPLETE;
        this.oauthId = "WITHDRAWN:" + this.id + ":" + this.oauthProvider.name();
        this.deleted = true;
    }

    // 온보딩 완료 처리
    public void completeOnboarding(CompleteOnboardingCommand command) {
        this.nickname = command.nickname();
        this.profileUrl = command.profileUrl();
        this.birthDate = command.birthDate();
        this.gender = command.gender();
        this.age = calculateAge(command.birthDate());
        this.height = command.height();
        this.weight = command.weight();
        this.onboardingStatus = OnboardingStatus.COMPLETE;
    }

    // 프로필 수정 요청값 중 입력된 값만 반영합니다.
    public void updateProfile(UpdateUserProfileCommand command) {
        if (command.profileUrl() != null) this.profileUrl = command.profileUrl();
        if (command.nickname() != null) this.nickname = command.nickname();
        if (command.gender() != null) this.gender = command.gender();
        if (command.birthDate() != null) {
            this.birthDate = command.birthDate();
            this.age = calculateAge(command.birthDate());
        }
        if (command.height() != null) this.height = command.height();
        if (command.weight() != null) this.weight = command.weight();
    }

    // 생년월일을 기준으로 만 나이를 계산합니다.
    private Integer calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

}
