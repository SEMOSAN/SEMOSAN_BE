package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import com.semosan.api.domain.user.repository.UserRepository;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;
    private final UserOnboardingRepository userOnboardingRepository;

    @Transactional(readOnly = true)
    public User findActiveUserById(Long userId) {
        if (userId == null) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserOnboarding findCompletedOnboardingByUserId(Long userId) {
        UserOnboarding userOnboarding = userOnboardingRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ONBOARDING_NOT_FOUND));

        if (userOnboarding.getUser().getOnboardingStatus() != OnboardingStatus.COMPLETE) {
            throw new GeneralException(ErrorStatus.ONBOARDING_NOT_COMPLETED);
        }
        return userOnboarding;
    }

    @Transactional(readOnly = true)
    public User findCompletedOnboardingUserById(Long userId) {
        return findCompletedOnboardingByUserId(userId).getUser();
    }
}
