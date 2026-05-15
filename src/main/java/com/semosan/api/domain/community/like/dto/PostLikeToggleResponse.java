package com.semosan.api.domain.community.like.dto;

public record PostLikeToggleResponse(
        boolean liked,
        long count
) {
}
