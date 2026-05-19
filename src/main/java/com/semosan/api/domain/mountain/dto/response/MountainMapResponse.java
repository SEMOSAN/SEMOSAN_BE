package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.repository.projection.MountainMapProjection;

public record MountainMapResponse(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        Boolean visited,
        Long visitCount,
        String imageUrl
) {

    public static MountainMapResponse from(MountainMapProjection projection) {
        long count = projection.getVisitCount() == null ? 0L : projection.getVisitCount();
        return new MountainMapResponse(
                projection.getId(),
                projection.getName(),
                projection.getLatitude(),
                projection.getLongitude(),
                count > 0,
                count,
                projection.getImageUrl()
        );
    }
}
