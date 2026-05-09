package com.semosan.api.domain.user.dto.response;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.user.Gender;

public record GetUserProfileResponse(
        String profileUrl,
        String nickname,
        Gender gender,
        Integer age,
        Double height,
        Double weight,
        ExerciseType exerciseType
) {
    // User와 온보딩 정보로 프로필 조회 응답 DTO를 생성합니다.
    public static GetUserProfileResponse of(User user, UserOnboarding userOnboarding) {
        return new GetUserProfileResponse(
                user.getProfileUrl(),
                user.getNickname(),
                user.getGender(),
                user.getAge(),
                user.getHeight(),
                user.getWeight(),
                userOnboarding == null ? null : userOnboarding.getExerciseType()
        );
    }
}
