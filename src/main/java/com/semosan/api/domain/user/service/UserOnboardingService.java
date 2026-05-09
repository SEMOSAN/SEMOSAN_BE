package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.dto.request.RegisterOnboardingRequest;
import com.semosan.api.domain.user.dto.response.GetUserProfileResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.FitnessLevel;
import com.semosan.api.domain.user.enums.HikingLevel;
import com.semosan.api.domain.user.repository.UserRepository;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserOnboardingService {

    private final UserOnboardingRepository userOnboardingRepository;
    private final UserRepository userRepository;
    private final FitnessLevelCalculator fitnessLevelCalculator;

    // 사용자 프로필과 온보딩 정보를 최초 1회 등록합니다.
    @Transactional
    public void registerUserOnboarding(Long userId, RegisterOnboardingRequest request) {
        User user = findActiveUserById(userId);
        validateOnboardingNotCompleted(user.getId());
        validatePreferredDifficulty(request);

        FitnessLevel fitnessLevel = fitnessLevelCalculator.calculate(request);
        user.completeOnboarding(toCompleteOnboardingCommand(request));
        createUserOnboarding(user, request, fitnessLevel);
    }

    // 로그인한 사용자의 프로필 정보를 조회합니다.
    @Transactional(readOnly = true)
    public GetUserProfileResponse getUserProfile(Long userId) {
        User user = findActiveUserById(userId);
        // 현재는 온보딩이 없는 사용자도 조회 성공해야 하므로 쿼리 2번 방식을 유지합니다.
        UserOnboarding userOnboarding = userOnboardingRepository.findByUser_Id(userId)
                .orElse(null);
        return GetUserProfileResponse.of(user, userOnboarding);
    }

    // 삭제되지 않은 활성 사용자를 조회합니다.
    private User findActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    // 사용자 온보딩 상세 정보를 저장합니다.
    public void createUserOnboarding(User user, RegisterOnboardingRequest request, FitnessLevel fitnessLevel) {
        UserOnboarding userOnboarding = UserOnboarding.create(
                toCreateUserOnboardingCommand(user, request, fitnessLevel)
        );
        try {
            userOnboardingRepository.save(userOnboarding);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ErrorStatus.ONBOARDING_ALREADY_COMPLETED);
        }
    }

    // 해당 사용자의 온보딩 정보가 이미 존재하는지 확인합니다.
    public boolean existsUserOnboarding(Long userId) {
        return userOnboardingRepository.existsByUser_Id(userId);
    }

    // 이미 온보딩 정보가 존재하면 중복 등록 예외를 발생시킵니다.
    private void validateOnboardingNotCompleted(Long userId) {
        if (existsUserOnboarding(userId)) {
            throw new GeneralException(ErrorStatus.ONBOARDING_ALREADY_COMPLETED);
        }
    }

    // 숙련자 여부에 따라 선호 난이도 입력값을 검증합니다. => 암의로 만든 메서드로 와프가 좀 더 세부화되면 그때 수정
    private void validatePreferredDifficulty(RegisterOnboardingRequest request) {
        if (request.hikingLevel() == HikingLevel.EXPERT && request.preferredDifficulty() == null) {
            throw new GeneralException(ErrorStatus.PREFERRED_DIFFICULTY_REQUIRED);
        }
        if (request.hikingLevel() != HikingLevel.EXPERT && request.preferredDifficulty() != null) {
            throw new GeneralException(ErrorStatus.PREFERRED_DIFFICULTY_NOT_ALLOWED);
        }
    }

    // 온보딩 요청 값을 User 엔티티 갱신용 command로 변환합니다.
    private CompleteOnboardingCommand toCompleteOnboardingCommand(RegisterOnboardingRequest request) {
        return new CompleteOnboardingCommand(
                request.nickname(),
                request.birthDate(),
                request.gender(),
                request.height(),
                request.weight()
        );
    }

    // 온보딩 요청 값을 UserOnboarding 엔티티 생성용 command로 변환합니다.
    private CreateUserOnboardingCommand toCreateUserOnboardingCommand(
            User user,
            RegisterOnboardingRequest request,
            FitnessLevel fitnessLevel
    ) {
        return new CreateUserOnboardingCommand(
                user,
                request.hikingLevel(),
                request.preferredDifficulty(),
                request.exerciseType(),
                request.exerciseFrequency(),
                request.exerciseDuration(),
                request.hikingGoalType(),
                request.hikingPurpose(),
                fitnessLevel
        );
    }
}
