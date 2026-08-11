package com.semosan.api.domain.hiking.repository.projection;

import java.time.LocalDateTime;

public interface UserHikingRecordProjection {

    Long getHikingRecordId();

    Long getSessionId();

    Long getMountainId();

    String getMountainName();

    Long getCourseId();

    String getCourseName();

    /** 자유기록에만 값이 있다. 화면 표시는 courseName ?? recordName. */
    String getRecordName();

    String getPhotoReportImageUrl();

    String getCliveImageUrl();

    Double getDistance();

    Integer getDuration();

    LocalDateTime getHikedAt();
}
