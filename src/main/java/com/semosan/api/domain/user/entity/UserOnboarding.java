package com.semosan.api.domain.user.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "user_onboardings")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboarding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "hiking_level", nullable = false, length = 20)
    private HikingLevel hikingLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 30)
    private ExerciseType exerciseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_frequency", length = 30)
    private ExerciseFrequency exerciseFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_duration", length = 30)
    private ExerciseDuration exerciseDuration;

    // command 값으로 UserOnboarding 엔티티를 생성합니다.
    public static UserOnboarding create(CreateUserOnboardingCommand command) {
        return UserOnboarding.builder()
                .user(command.user())
                .hikingLevel(command.hikingLevel())
                .exerciseType(command.exerciseType())
                .exerciseFrequency(command.exerciseFrequency())
                .exerciseDuration(command.exerciseDuration())
                .build();
    }

    // 등산 경험 레벨을 변경합니다.
    public void updateHikingLevel(HikingLevel hikingLevel) {
        this.hikingLevel = hikingLevel;
    }

    // 주로 하는 운동 종류를 변경합니다.
    public void updateExerciseType(ExerciseType exerciseType) {
        this.exerciseType = exerciseType;
        if (exerciseType == ExerciseType.NONE) {
            this.exerciseFrequency = null;
            this.exerciseDuration = null;
        }
    }
}
