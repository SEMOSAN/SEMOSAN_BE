package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    public User findCompletedOnboardingUserById(Long userId) {
        User user = findActiveUserById(userId);
//        if (user.getOnboardingStatus() != OnboardingStatus.COMPLETE) {
//            throw new GeneralException(ErrorStatus.ONBOARDING_NOT_COMPLETED);
//        }
        return user;
    }
}
