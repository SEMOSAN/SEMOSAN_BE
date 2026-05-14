package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.user.entity.User;

public record AuthorResponse(
        Long id,
        String name,
        String profileUrl,
        boolean isDeleted
) {

    //TODO: 앱 정책에 따라서 탈퇴한 회원 이름 어떻게 보내줄지 결정되면 통일하기
    private static final String DELETED_USER_NAME = "탈퇴한 사용자";
    private static final String UNKNOWN_USER_NAME = "이름없음";

    public static AuthorResponse from(User user) {
        if (user.isDeleted()) {
            return new AuthorResponse(user.getId(), DELETED_USER_NAME, null, true);
        }
        String name = user.getName() != null ? user.getName() : UNKNOWN_USER_NAME;
        return new AuthorResponse(user.getId(), name, user.getProfileUrl(), false);
    }
}
