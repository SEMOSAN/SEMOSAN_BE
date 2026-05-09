package com.semosan.api.domain.user.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.enums.ExerciseDuration;
import com.semosan.api.domain.user.enums.ExerciseFrequency;
import com.semosan.api.domain.user.enums.ExerciseType;
import com.semosan.api.domain.user.enums.FitnessLevel;
import com.semosan.api.domain.user.enums.HikingGoalType;
import com.semosan.api.domain.user.enums.HikingLevel;
import com.semosan.api.domain.user.enums.HikingPurpose;
import com.semosan.api.domain.user.enums.PreferredDifficulty;
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
    @Column(name = "preferred_difficulty", length = 20)
    private PreferredDifficulty preferredDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 30)
    private ExerciseType exerciseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_frequency", nullable = false, length = 30)
    private ExerciseFrequency exerciseFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_duration", nullable = false, length = 30)
    private ExerciseDuration exerciseDuration;

    @Enumerated(EnumType.STRING)
    @Column(name = "hiking_goal_type", nullable = false, length = 30)
    private HikingGoalType hikingGoalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "hiking_purpose", nullable = false, length = 30)
    private HikingPurpose hikingPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_level", nullable = false, length = 20)
    private FitnessLevel fitnessLevel;

    // command 값으로 UserOnboarding 엔티티를 생성합니다.
    public static UserOnboarding create(CreateUserOnboardingCommand command) {
        return UserOnboarding.builder()
                .user(command.user())
                .hikingLevel(command.hikingLevel())
                .preferredDifficulty(command.preferredDifficulty())
                .exerciseType(command.exerciseType())
                .exerciseFrequency(command.exerciseFrequency())
                .exerciseDuration(command.exerciseDuration())
                .hikingGoalType(command.hikingGoalType())
                .hikingPurpose(command.hikingPurpose())
                .fitnessLevel(command.fitnessLevel())
                .build();
    }
}
