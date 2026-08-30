package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.hiking.repository.CourseDifficultyFeedbackRepository;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.notification.repository.NotificationRepository;
import com.semosan.api.domain.review.repository.ReviewRepository;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.dto.command.OAuthUserProfile;
import com.semosan.api.domain.user.dto.command.UpdateUserProfileCommand;
import com.semosan.api.domain.user.dto.request.UpdateUserProfileRequest;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserNotificationSetting;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.event.UserRegisteredEvent;
import com.semosan.api.domain.user.policy.DefaultNicknameGenerator;
import com.semosan.api.domain.user.policy.NicknamePolicy;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final MountainLikeRepository mountainLikeRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final ReviewRepository reviewRepository;
    private final HikingMemberRepository hikingMemberRepository;
    private final HikingRecordRepository hikingRecordRepository;
    private final CourseDifficultyFeedbackRepository courseDifficultyFeedbackRepository;
    private final NotificationRepository notificationRepository;
    private final DefaultNicknameGenerator defaultNicknameGenerator;
    private final NicknamePolicy nicknamePolicy;
    private final UserReader userReader;
    private final ApplicationEventPublisher eventPublisher;

    // OAuth 유저 조회 후 없으면 신규 생성합니다.
    @Transactional
    public User findOrRegisterOAuthUser(
            OAuthUserProfile profile, OAuthProvider provider, DeviceType deviceType
    ) {
        return userRepository.findByOauthIdAndOauthProvider(profile.oauthId(), provider)
                .filter(user -> !user.isDeleted())
                .orElseGet(() -> registerOAuthUser(profile, provider, deviceType));
    }

    // OAuth 신규 가입 유저를 생성하고 가입 알림 이벤트를 발행합니다.
    private User registerOAuthUser(
            OAuthUserProfile profile, OAuthProvider provider, DeviceType deviceType
    ) {
        User savedUser = saveNewUserWithNotificationSetting(
                User.createOAuthUser(
                        profile.oauthId(),
                        profile.email(),
                        profile.name(),
                        deviceType,
                        provider
                )
        );
        // 테스트 로그인으로 생성되는 유저는 알림 대상에서 제외합니다.
        eventPublisher.publishEvent(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getNickname(),
                provider,
                deviceType,
                savedUser.getCreatedAt()
        ));
        return savedUser;
    }

    // 테스트 유저 조회 후 없으면 신규 생성합니다.
    @Transactional
    public User findOrCreateTestUser(String testUserId, DeviceType deviceType) {
        return userRepository.findByOauthIdAndOauthProvider(testUserId, OAuthProvider.TEST)
                .filter(user -> !user.isDeleted())
                .orElseGet(() -> saveNewUserWithNotificationSetting(User.createTestUser(testUserId, deviceType)));
    }

    // 신규 유저 저장 후 기본 알림 설정을 생성합니다.
    private User saveNewUserWithNotificationSetting(User user) {
        user.updateNickname(defaultNicknameGenerator.generate());
        User savedUser = userRepository.save(user);
        // 온보딩 권한 설정 초기화 전에 항상 기본 알림 설정 row가 존재하도록 생성합니다.
        userNotificationSettingRepository.save(UserNotificationSetting.createDefault(savedUser));
        return savedUser;
    }

    // 닉네임 사용 가능 여부를 조회합니다.
    @Transactional(readOnly = true)
    public void checkNickname(Long userId, String nickname) {
        userReader.findActiveUserById(userId);
        nicknamePolicy.validate(nickname);
    }

    // 로그인한 사용자의 프로필 정보를 수정합니다.
    @Transactional
    public void updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        validateProfileUpdateRequest(request);
        validateNicknameIfPresent(request.nickname());
        User user = userReader.findActiveUserById(userId);
        user.updateProfile(toUpdateUserProfileCommand(request));
        updateUserOnboardingProfile(user, request);
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
    private void updateUserOnboardingProfile(User user, UpdateUserProfileRequest request) {
        if (request.hikingLevel() == null && request.exerciseType() == null) {
            return;
        }
        UserOnboarding userOnboarding = userOnboardingRepository.findByUser_Id(user.getId())
                .orElseGet(() -> userOnboardingRepository.save(UserOnboarding.create(
                        new CreateUserOnboardingCommand(
                                user,
                                null,
                                null,
                                null,
                                null
                        )
                )));
        if (request.hikingLevel() != null) {
            userOnboarding.updateHikingLevel(request.hikingLevel());
        }
        if (request.exerciseType() != null) {
            userOnboarding.updateExerciseType(request.exerciseType());
        }
    }

    // 로그인한 사용자를 탈퇴 처리하고 하위 데이터를 삭제합니다.
    @Transactional
    public void withdrawUser(User user) {
        deleteUserChildRecords(user.getId());
        user.withdraw();
        userRepository.save(user);
    }

    private void deleteUserChildRecords(Long userId) {
        mountainLikeRepository.deleteByUser_Id(userId);
        courseLikeRepository.deleteByUser_Id(userId);
        reviewRepository.deleteByUser_Id(userId);
        courseDifficultyFeedbackRepository.deleteByUserId(userId);
        List<Long> recordIdsToDelete = hikingRecordRepository.findRecordIdsOnlyParticipatedByUser(userId);
        hikingMemberRepository.deleteByUser_Id(userId);
        if (!recordIdsToDelete.isEmpty()) {
            courseDifficultyFeedbackRepository.deleteByHikingRecordIdIn(recordIdsToDelete);
            hikingRecordRepository.deleteAllByIdInBatch(recordIdsToDelete);
        }
        notificationRepository.deleteAllByUserId(userId);
        userOnboardingRepository.deleteByUser_Id(userId);
        userNotificationSettingRepository.deleteByUser_Id(userId);
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
