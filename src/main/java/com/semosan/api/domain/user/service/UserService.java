package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.dto.command.UpdateUserProfileCommand;
import com.semosan.api.domain.user.dto.request.UpdateUserProfileRequest;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserNotificationSetting;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.policy.NicknamePolicy;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final NicknamePolicy nicknamePolicy;

    // 카카오 유저 조회 후 없으면 신규 생성, 탈퇴 유저면 복구
    @Transactional
    public User findOrRegisterKakaoUser(
            String kakaoId, String email, String name, String profileUrl, DeviceType deviceType
    ) {
        return userRepository.findByOauthIdAndOauthProvider(kakaoId, OAuthProvider.KAKAO)
                .map(user -> {
                    if (user.isDeleted())
                        user.restore(email, name, profileUrl, deviceType);
                    return user;
                })
                .orElseGet(() -> saveNewUserWithNotificationSetting(
                        User.createKakaoUser(kakaoId, email, name, profileUrl, deviceType)
                ));
    }

    // 애플 유저 조회 후 없으면 신규 생성, 탈퇴 유저면 복구
    @Transactional
    public User findOrRegisterAppleUser(String appleId, String email, String name, DeviceType deviceType) {
        return userRepository.findByOauthIdAndOauthProvider(appleId, OAuthProvider.APPLE)
                .map(user -> {
                    if (user.isDeleted())
                        user.restore(email, name, null, deviceType);
                    return user;
                })
                .orElseGet(() -> saveNewUserWithNotificationSetting(
                        User.createAppleUser(appleId, email, name, deviceType)
                ));
    }

    // userId로 삭제되지 않은 유저를 조회하고, 없으면 예외를 발생시킵니다.
    public User findActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    // userId로 삭제되지 않은 유저를 조회하고, 없으면 예외를 발생시킵니다.
    @Transactional(readOnly = true)
    public User findActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    // 테스트 유저 조회 후 없으면 신규 생성
    @Transactional
    public User findOrCreateTestUser(String testUserId, DeviceType deviceType) {
        return userRepository.findByOauthIdAndOauthProvider(testUserId, OAuthProvider.TEST)
                .orElseGet(() -> saveNewUserWithNotificationSetting(User.createTestUser(testUserId, deviceType)));
    }

    // 신규 유저 저장 후 기본 알림 설정을 생성합니다.
    private User saveNewUserWithNotificationSetting(User user) {
        User savedUser = userRepository.save(user);
        // 온보딩 권한 설정 초기화 전에 항상 기본 알림 설정 row가 존재하도록 생성합니다.
        userNotificationSettingRepository.save(UserNotificationSetting.createDefault(savedUser));
        return savedUser;
    }

    // 닉네임 사용 가능 여부를 조회합니다.
    @Transactional(readOnly = true)
    public void checkNickname(String nickname) {
        nicknamePolicy.validate(nickname);
    }

    // 로그인한 사용자의 프로필 정보를 수정합니다.
    @Transactional
    public void updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        validateProfileUpdateRequest(request);
        validateNicknameIfPresent(request.nickname());
        User user = findActiveUserById(userId);
        user.updateProfile(toUpdateUserProfileCommand(request));
        updateUserOnboardingProfile(userId, request);
    }

    // 프로필 수정 요청에 최소 하나 이상의 수정 값이 있는지 검증합니다.
    private void validateProfileUpdateRequest(UpdateUserProfileRequest request) {
        if (request.profileUrl() == null
                && request.nickname() == null
                && request.gender() == null
                && request.birthDate() == null
                && request.height() == null
                && request.weight() == null
                && request.hikingLevel() == null
                && request.exerciseType() == null) {
            throw new GeneralException(ErrorStatus.PROFILE_UPDATE_FIELD_REQUIRED);
        }
    }

    // 닉네임 수정 값이 있으면 형식, 금칙어, 중복 여부를 검증합니다.
    private void validateNicknameIfPresent(String nickname) {
        if (nickname != null) {
            nicknamePolicy.validate(nickname);
        }
    }

    // 프로필 수정 요청에 포함된 온보딩 항목을 갱신합니다.
    private void updateUserOnboardingProfile(Long userId, UpdateUserProfileRequest request) {
        if (request.hikingLevel() == null && request.exerciseType() == null) {
            return;
        }
        UserOnboarding userOnboarding = userOnboardingRepository.findByUser_Id(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ONBOARDING_NOT_FOUND));
        if (request.hikingLevel() != null) {
            userOnboarding.updateHikingLevel(request.hikingLevel());
        }
        if (request.exerciseType() != null) {
            userOnboarding.updateExerciseType(request.exerciseType());
        }
    }

    // 프로필 수정 요청 값을 User 엔티티 갱신용 command로 변환합니다.
    private UpdateUserProfileCommand toUpdateUserProfileCommand(UpdateUserProfileRequest request) {
        return new UpdateUserProfileCommand(
                request.profileUrl(),
                request.nickname(),
                request.gender(),
                request.birthDate(),
                request.height(),
                request.weight()
        );
    }

}
