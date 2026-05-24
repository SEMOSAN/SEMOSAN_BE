package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.Gender;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReaderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserOnboardingRepository userOnboardingRepository;

    @Test
    void findCompletedOnboardingByUserIdReturnsOnboardingWhenUserIsCompleted() {
        User user = user();
        UserOnboarding onboarding = onboarding(user);
        when(userOnboardingRepository.findByUserIdWithUser(1L)).thenReturn(Optional.of(onboarding));

        UserOnboarding result = new UserReader(userRepository, userOnboardingRepository)
                .findCompletedOnboardingByUserId(1L);

        assertThat(result).isSameAs(onboarding);
        assertThat(result.getUser()).isSameAs(user);
    }

    @Test
    void findCompletedOnboardingByUserIdThrowsWhenOnboardingMissing() {
        when(userOnboardingRepository.findByUserIdWithUser(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UserReader(userRepository, userOnboardingRepository)
                .findCompletedOnboardingByUserId(1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ONBOARDING_NOT_FOUND);
    }

    @Test
    void findCompletedOnboardingByUserIdThrowsWhenUserNotCompleted() {
        User user = User.createTestUser("reader-test-user", DeviceType.IOS);
        UserOnboarding onboarding = onboarding(user);
        when(userOnboardingRepository.findByUserIdWithUser(1L)).thenReturn(Optional.of(onboarding));

        assertThatThrownBy(() -> new UserReader(userRepository, userOnboardingRepository)
                .findCompletedOnboardingByUserId(1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ONBOARDING_NOT_COMPLETED);
    }

    private User user() {
        User user = User.createTestUser("reader-test-user", DeviceType.IOS);
        user.completeOnboarding(new CompleteOnboardingCommand(
                "테스트",
                null,
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                175.0,
                70.0
        ));
        return user;
    }

    private UserOnboarding onboarding(User user) {
        return UserOnboarding.create(new CreateUserOnboardingCommand(
                user,
                HikingLevel.EXPERT,
                ExerciseType.HIKING,
                ExerciseFrequency.DAILY,
                ExerciseDuration.OVER_4H
        ));
    }
}
