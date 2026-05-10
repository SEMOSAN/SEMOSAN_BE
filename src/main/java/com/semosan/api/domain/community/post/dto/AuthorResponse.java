package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.user.entity.User;

public record AuthorResponse(
        Long id,
        String name,
        String profileUrl
) {
    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.getId(), user.getName(), user.getProfileUrl());
    }
}
