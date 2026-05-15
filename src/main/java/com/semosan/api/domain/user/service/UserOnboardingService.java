package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.dto.request.RegisterOnboardingRequest;
import com.semosan.api.domain.user.dto.response.GetUserProfileResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserNotificationSetting;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.policy.NicknamePolicy;
import com.semosan.api.domain.user.repository.UserNotificationSettingRepository;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class UserOnboardingService {

    private final UserOnboardingRepository userOnboardingRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final NicknamePolicy nicknamePolicy;
    private final UserReader userReader;

    // 사용자 프로필과 온보딩 정보를 최초 1회 등록합니다.
    @Transactional
    public void registerUserOnboarding(Long userId, RegisterOnboardingRequest request) {
        User user = userReader.findActiveUserById(userId);
        validateOnboardingNotCompleted(user.getId());
        nicknamePolicy.validate(request.nickname());
        validateBirthDate(request.birthDate());
        validateExerciseDetail(request);

        user.completeOnboarding(toCompleteOnboardingCommand(request));
        initializeNotificationSetting(user.getId(), request);
        createUserOnboarding(user, request);
    }

    // 로그인한 사용자의 프로필 정보를 조회합니다.
    @Transactional(readOnly = true)
    public GetUserProfileResponse getUserProfile(Long userId) {
        User user = userReader.findActiveUserById(userId);
        // 현재는 온보딩이 없는 사용자도 조회 성공해야 하므로 쿼리 2번 방식을 유지합니다.
        UserOnboarding userOnboarding = userOnboardingRepository.findByUser_Id(userId)
                .orElse(null);
        return GetUserProfileResponse.of(user, userOnboarding);
    }

    // 사용자 온보딩 상세 정보를 저장합니다.
    private void createUserOnboarding(User user, RegisterOnboardingRequest request) {
        UserOnboarding userOnboarding = UserOnboarding.create(
                toCreateUserOnboardingCommand(user, request)
        );
        try {
            userOnboardingRepository.save(userOnboarding);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ErrorStatus.ONBOARDING_ALREADY_COMPLETED);
        }
    }

    // 해당 사용자의 온보딩 정보가 이미 존재하는지 확인합니다.
    private boolean existsUserOnboarding(Long userId) {
        return userOnboardingRepository.existsByUser_Id(userId);
    }

    // 온보딩에서 선택한 권한 설정값으로 사용자 알림 설정을 초기화합니다.
    private void initializeNotificationSetting(Long userId, RegisterOnboardingRequest request) {
        UserNotificationSetting setting = userNotificationSettingRepository.findByUser_Id(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_SETTING_NOT_FOUND));
        setting.updatePushNotification(request.pushNotificationEnabled());
        setting.updateLiveActivity(request.liveActivityEnabled());
        setting.updateVoice(request.voiceEnabled());
    }

    // 이미 온보딩 정보가 존재하면 중복 등록 예외를 발생시킵니다.
    private void validateOnboardingNotCompleted(Long userId) {
        if (existsUserOnboarding(userId)) {
            throw new GeneralException(ErrorStatus.ONBOARDING_ALREADY_COMPLETED);
        }
    }

    // 선택한 운동 종류에 따라 운동 빈도와 운동 시간 입력 여부를 검증합니다.
    private void validateExerciseDetail(RegisterOnboardingRequest request) {
        // 운동 안함 선택 시 운동 빈도와 운동 시간은 둘 다 입력하지 않아야 합니다.
        boolean hasExerciseDetail = request.exerciseFrequency() != null || request.exerciseDuration() != null;
        if (request.exerciseType() == ExerciseType.NONE && hasExerciseDetail) {
            throw new GeneralException(ErrorStatus.EXERCISE_DETAIL_NOT_ALLOWED);
        }
        if (request.exerciseType() != ExerciseType.NONE
                && (request.exerciseFrequency() == null || request.exerciseDuration() == null)) {
            throw new GeneralException(ErrorStatus.EXERCISE_DETAIL_REQUIRED);
        }
    }

    // 생년월일 기준 만 14세 이상인지 검증합니다.
    private void validateBirthDate(LocalDate birthDate) {
        if (Period.between(birthDate, LocalDate.now()).getYears() < 14) {
            throw new GeneralException(ErrorStatus.UNDER_AGE_NOT_ALLOWED);
        }
    }

    // 온보딩 요청 값을 User 엔티티 갱신용 command로 변환합니다.
    private CompleteOnboardingCommand toCompleteOnboardingCommand(RegisterOnboardingRequest request) {
        return new CompleteOnboardingCommand(
                request.nickname(),
                request.profileUrl(),
                request.birthDate(),
                request.gender(),
                request.height(),
                request.weight()
        );
    }

    // 온보딩 요청 값을 UserOnboarding 엔티티 생성용 command로 변환합니다.
    private CreateUserOnboardingCommand toCreateUserOnboardingCommand(
            User user,
            RegisterOnboardingRequest request
    ) {
        return new CreateUserOnboardingCommand(
                user,
                request.hikingLevel(),
                request.exerciseType(),
                request.exerciseFrequency(),
                request.exerciseDuration()
        );
    }
}
