package com.semosan.api.domain.hiking.repository.projection;

import java.time.LocalDateTime;

public interface UserHikingRecordProjection {

    Long getMountainId();

    String getMountainName();

    String getImageUrl();

    Long getHikingCount();

    LocalDateTime getLastHikedAt();
}
