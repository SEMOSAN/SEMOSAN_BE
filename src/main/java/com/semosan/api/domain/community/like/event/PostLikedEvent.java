package com.semosan.api.domain.community.like.event;

public record PostLikedEvent(Long postId, Long actorId) {
}
