package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.user.entity.User;

public record AuthorResponse(
        Long id,
        String nickname,
        String profileUrl,
        boolean isDeleted
) {

    private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자";
    private static final String UNKNOWN_USER_NICKNAME = "이름없음";

    public static AuthorResponse from(User user) {
        if (user.isDeleted()) {
            return new AuthorResponse(user.getId(), DELETED_USER_NICKNAME, null, true);
        }
        String nickname = user.getNickname() != null ? user.getNickname() : UNKNOWN_USER_NICKNAME;
        return new AuthorResponse(user.getId(), nickname, user.getProfileUrl(), false);
    }
}
