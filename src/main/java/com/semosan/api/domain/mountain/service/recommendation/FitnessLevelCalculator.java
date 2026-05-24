package com.semosan.api.domain.mountain.service.recommendation;

import com.semosan.api.domain.mountain.enums.FitnessLevel;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.Gender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class FitnessLevelCalculator {

    public FitnessLevel calculate(User user, UserOnboarding onboarding) {
        if (onboarding.getExerciseType() == ExerciseType.NONE) {
            return FitnessLevel.ENTRY;
        }

        double totalScore = hikingExperienceScore(onboarding.getHikingLevel())
                + exerciseScore(onboarding)
                + ageAdjustment(user.getBirthDate())
                + bmiAdjustment(user.getGender(), user.getHeight(), user.getWeight());

        FitnessLevel level = scoreToLevel(totalScore);

        if (onboarding.getHikingLevel() == HikingLevel.BEGINNER && level.ordinal() > FitnessLevel.BEGINNER.ordinal()) {
            return FitnessLevel.BEGINNER;
        }
        if (onboarding.getHikingLevel() == HikingLevel.EXPERT && totalScore < 55) {
            return FitnessLevel.INTERMEDIATE;
        }
        return level;
    }

    private double hikingExperienceScore(HikingLevel hikingLevel) {
        return switch (hikingLevel) {
            case BEGINNER -> 10;
            case EXPERIENCED -> 22;
            case HOBBY -> 34;
            case EXPERT -> 45;
        };
    }

    private double exerciseScore(UserOnboarding onboarding) {
        ExerciseFrequency frequency = onboarding.getExerciseFrequency();
        ExerciseDuration duration = onboarding.getExerciseDuration();
        if (frequency == null || duration == null) {
            return 0;
        }
        return Math.min(35, 35
                * exerciseTypeWeight(onboarding.getExerciseType())
                * exerciseFrequencyMultiplier(frequency)
                * exerciseDurationMultiplier(duration));
    }

    private double exerciseTypeWeight(ExerciseType exerciseType) {
        return switch (exerciseType) {
            case HIKING -> 1.00;
            case CROSSFIT -> 0.95;
            case RUNNING -> 0.90;
            case SPORTS -> 0.75;
            case GYM -> 0.70;
            case SWIMMING -> 0.65;
            case HOME_TRAINING -> 0.55;
            case PILATES_YOGA -> 0.45;
            case WALKING -> 0.40;
            case NONE -> 0;
        };
    }

    private double exerciseFrequencyMultiplier(ExerciseFrequency frequency) {
        return switch (frequency) {
            case LESS_THAN_MONTH_1 -> 0.35;
            case MONTH_1_2 -> 0.55;
            case WEEK_1_2 -> 0.75;
            case WEEK_3_4 -> 1.00;
            case DAILY -> 1.10;
        };
    }

    private double exerciseDurationMultiplier(ExerciseDuration duration) {
        return switch (duration) {
            case UNDER_1H -> 0.70;
            case HOUR_1_2 -> 0.90;
            case HOUR_2_4 -> 1.00;
            case OVER_4H -> 1.10;
        };
    }

    private int ageAdjustment(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age >= 20 && age <= 39) {
            return 2;
        }
        if ((age >= 15 && age <= 19) || (age >= 40 && age <= 54)) {
            return 1;
        }
        return 0;
    }

    private int bmiAdjustment(Gender gender, Double height, Double weight) {
        if (gender == null || gender == Gender.NONE || height == null || weight == null || height <= 0) {
            return 0;
        }

        double heightMeter = height / 100;
        double bmi = weight / (heightMeter * heightMeter);
        return switch (gender) {
            case FEMALE -> femaleBmiAdjustment(bmi);
            case MALE -> maleBmiAdjustment(bmi);
            case NONE -> 0;
        };
    }

    private int femaleBmiAdjustment(double bmi) {
        if (bmi >= 18.5 && bmi < 23) {
            return 3;
        }
        if (bmi >= 23 && bmi < 25) {
            return 1;
        }
        if ((bmi >= 17 && bmi < 18.5) || (bmi >= 25 && bmi < 30)) {
            return 0;
        }
        return -2;
    }

    private int maleBmiAdjustment(double bmi) {
        if (bmi >= 20 && bmi < 25) {
            return 3;
        }
        if (bmi >= 25 && bmi < 27) {
            return 1;
        }
        if ((bmi >= 18.5 && bmi < 20) || (bmi >= 27 && bmi < 30)) {
            return 0;
        }
        return -2;
    }

    private FitnessLevel scoreToLevel(double totalScore) {
        if (totalScore >= 75) {
            return FitnessLevel.ADVANCED;
        }
        if (totalScore >= 55) {
            return FitnessLevel.INTERMEDIATE;
        }
        if (totalScore >= 35) {
            return FitnessLevel.BEGINNER;
        }
        return FitnessLevel.ENTRY;
    }
}
