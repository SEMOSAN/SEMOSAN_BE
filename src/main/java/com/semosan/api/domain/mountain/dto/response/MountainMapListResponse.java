package com.semosan.api.domain.mountain.dto.response;

import java.util.List;

public record MountainMapListResponse(
        Boolean hasHikingRecord,
        List<MountainMapResponse> mountains
) {

    public static MountainMapListResponse of(boolean hasHikingRecord, List<MountainMapResponse> mountains) {
        return new MountainMapListResponse(hasHikingRecord, mountains);
    }
}
