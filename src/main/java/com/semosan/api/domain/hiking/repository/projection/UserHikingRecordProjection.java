package com.semosan.api.domain.hiking.repository.projection;

import java.time.LocalDateTime;

public interface UserHikingRecordProjection {

    Long getHikingRecordId();

    Long getSessionId();

    Long getMountainId();

    String getMountainName();

    Long getCourseId();

    String getCourseName();

    String getPhotoReportImageUrl();

    String getCliveImageUrl();

    Double getDistance();

    Integer getDuration();

    LocalDateTime getHikedAt();
}
