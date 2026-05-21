package com.semosan.api.domain.user.dto.response;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.Gender;

import java.time.LocalDate;

public record GetUserProfileResponse(
        String profileUrl,
        String name,
        String nickname,
        HikingLevel hikingLevel,
        Gender gender,
        Integer age,
        Double height,
        Double weight,
        ExerciseType exerciseType,
        LocalDate birthDate
) {
    // User와 온보딩 정보로 프로필 조회 응답 DTO를 생성합니다.
    public static GetUserProfileResponse of(User user, UserOnboarding userOnboarding) {
        HikingLevel hikingLevel = null;
        ExerciseType exerciseType = null;
        if (userOnboarding != null) {
            hikingLevel = userOnboarding.getHikingLevel();
            exerciseType = userOnboarding.getExerciseType();
        }

        return new GetUserProfileResponse(
                user.getProfileUrl(),
                user.getName(),
                user.getNickname(),
                hikingLevel,
                user.getGender(),
                user.getAge(),
                user.getHeight(),
                user.getWeight(),
                exerciseType,
                user.getBirthDate()
        );
    }
}
