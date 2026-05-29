package com.semosan.api.domain.mountain.dto.response;

public record CourseLikeToggleResponse(
        boolean liked,
        long count
) {
}
