package com.semosan.api.domain.hiking.repository.projection;

import java.time.LocalDateTime;

public interface UserHikingMountainRecordProjection {

    Long getMountainId();

    String getMountainName();

    String getImageUrl1();

    String getImageUrl2();

    Long getHikingCount();

    LocalDateTime getLastHikedAt();
}
