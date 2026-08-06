package com.semosan.api.domain.user.entity;

import com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand;
import com.semosan.api.domain.user.dto.command.UpdateUserProfileCommand;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.Gender;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void createOAuthUserInitializesIncompleteActiveUser() {
        User user = User.createOAuthUser(
                "oauth-id",
                "user@example.com",
                "사용자",
                DeviceType.IOS,
                OAuthProvider.KAKAO
        );

        assertThat(user.getOauthId()).isEqualTo("oauth-id");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getName()).isEqualTo("사용자");
        assertThat(user.getDeviceType()).isEqualTo(DeviceType.IOS);
        assertThat(user.getOauthProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(user.getOnboardingStatus()).isEqualTo(OnboardingStatus.INCOMPLETE);
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void createTestUserInitializesTestProviderUser() {
        User user = User.createTestUser("test-user", DeviceType.ANDROID);

        assertThat(user.getOauthId()).isEqualTo("test-user");
        assertThat(user.getOauthProvider()).isEqualTo(OAuthProvider.TEST);
        assertThat(user.getEmail()).isEqualTo("test-user@test.com");
        assertThat(user.getName()).isEqualTo("테스트유저_test-user");
        assertThat(user.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(user.getOnboardingStatus()).isEqualTo(OnboardingStatus.INCOMPLETE);
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void withdrawClearsProfileAndMarksDeleted() {
        User user = completedUser();
        ReflectionTestUtils.setField(user, "id", 1L);

        user.withdraw();

        assertThat(user.getEmail()).isNull();
        assertThat(user.getName()).isNull();
        assertThat(user.getNickname()).isNull();
        assertThat(user.getProfileUrl()).isNull();
        assertThat(user.getGender()).isNull();
        assertThat(user.getAge()).isNull();
        assertThat(user.getBirthDate()).isNull();
        assertThat(user.getHeight()).isNull();
        assertThat(user.getWeight()).isNull();
        assertThat(user.getOnboardingStatus()).isEqualTo(OnboardingStatus.INCOMPLETE);
        assertThat(user.getOauthId()).isEqualTo("WITHDRAWN:1:TEST");
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    void updateNicknameChangesNickname() {
        User user = User.createTestUser("nickname-user", DeviceType.IOS);

        user.updateNickname("새닉네임");

        assertThat(user.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    void displayNameReturnsNicknameWhenNicknameHasText() {
        User user = User.createTestUser("display-user", DeviceType.IOS);
        user.updateNickname("닉네임");

        assertThat(user.displayName()).isEqualTo("닉네임");
    }

    @Test
    void displayNameReturnsNameWhenNicknameIsBlank() {
        User user = User.createOAuthUser("oauth-id", "user@example.com", "실명", DeviceType.IOS, OAuthProvider.KAKAO);
        user.updateNickname(" ");

        assertThat(user.displayName()).isEqualTo("실명");
    }

    @Test
    void displayNameReturnsDefaultWhenNicknameAndNameAreBlank() {
        User user = User.createOAuthUser("oauth-id", "user@example.com", " ", DeviceType.IOS, OAuthProvider.KAKAO);

        assertThat(user.displayName()).isEqualTo("사용자");
    }

    @Test
    void displayNameReturnsDefaultWhenNicknameAndNameAreNull() {
        User user = User.createTestUser("display-user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "name", null);

        assertThat(user.displayName()).isEqualTo("사용자");
    }

    @Test
    void completeOnboardingUpdatesProfileAndAge() {
        User user = User.createTestUser("onboarding-user", DeviceType.IOS);
        LocalDate birthDate = LocalDate.now().minusYears(25);

        user.completeOnboarding(new CompleteOnboardingCommand(
                "푸름",
                "https://example.com/profile.png",
                birthDate,
                Gender.FEMALE,
                170.0,
                60.0
        ));

        assertThat(user.getNickname()).isEqualTo("푸름");
        assertThat(user.getProfileUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(user.getBirthDate()).isEqualTo(birthDate);
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.getAge()).isEqualTo(25);
        assertThat(user.getHeight()).isEqualTo(170.0);
        assertThat(user.getWeight()).isEqualTo(60.0);
        assertThat(user.getOnboardingStatus()).isEqualTo(OnboardingStatus.COMPLETE);
    }

    @Test
    void updateProfileAppliesOnlyNonNullFields() {
        User user = completedUser();
        LocalDate newBirthDate = LocalDate.now().minusYears(30);

        user.updateProfile(new UpdateUserProfileCommand(
                null,
                "변경닉네임",
                null,
                newBirthDate,
                null,
                65.0
        ));

        assertThat(user.getProfileUrl()).isEqualTo("https://example.com/original.png");
        assertThat(user.getNickname()).isEqualTo("변경닉네임");
        assertThat(user.getGender()).isEqualTo(Gender.MALE);
        assertThat(user.getBirthDate()).isEqualTo(newBirthDate);
        assertThat(user.getAge()).isEqualTo(30);
        assertThat(user.getHeight()).isEqualTo(175.0);
        assertThat(user.getWeight()).isEqualTo(65.0);
    }

    @Test
    void updateProfileAppliesProfileGenderAndHeightWhenProvided() {
        User user = completedUser();

        user.updateProfile(new UpdateUserProfileCommand(
                "https://example.com/new.png",
                null,
                Gender.FEMALE,
                null,
                180.0,
                null
        ));

        assertThat(user.getProfileUrl()).isEqualTo("https://example.com/new.png");
        assertThat(user.getNickname()).isEqualTo("기존닉네임");
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.now().minusYears(20));
        assertThat(user.getHeight()).isEqualTo(180.0);
        assertThat(user.getWeight()).isEqualTo(70.0);
    }

    @Test
    void isSuspendedReturnsFalseWhenSuspendedUntilIsNull() {
        User user = User.createTestUser("suspend-user", DeviceType.IOS);

        assertThat(user.isSuspended()).isFalse();
    }

    @Test
    void isSuspendedReturnsTrueWhenSuspensionIsInFuture() {
        User user = User.createTestUser("suspend-user", DeviceType.IOS);
        user.suspend(LocalDateTime.now().plusDays(1));

        assertThat(user.isSuspended()).isTrue();
    }

    @Test
    void isSuspendedReturnsFalseWhenSuspensionIsExpired() {
        User user = User.createTestUser("suspend-user", DeviceType.IOS);
        user.suspend(LocalDateTime.now().minusSeconds(1));

        assertThat(user.isSuspended()).isFalse();
    }

    @Test
    void unsuspendClearsSuspension() {
        User user = User.createTestUser("suspend-user", DeviceType.IOS);
        user.suspend(LocalDateTime.now().plusDays(1));

        user.unsuspend();

        assertThat(user.getSuspendedUntil()).isNull();
        assertThat(user.isSuspended()).isFalse();
    }

    private User completedUser() {
        User user = User.createTestUser("completed-user", DeviceType.IOS);
        user.completeOnboarding(new CompleteOnboardingCommand(
                "기존닉네임",
                "https://example.com/original.png",
                LocalDate.now().minusYears(20),
                Gender.MALE,
                175.0,
                70.0
        ));
        return user;
    }
}
