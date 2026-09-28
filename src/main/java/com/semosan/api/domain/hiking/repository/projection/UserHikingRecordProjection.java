package com.semosan.api.domain.hiking.repository.projection;

import java.time.LocalDateTime;

public interface UserHikingRecordProjection {

    Long getHikingRecordId();

    Long getSessionId();

    Long getMountainId();

    String getMountainName();

    Long getCourseId();

    String getCourseName();

    /** 사용자가 정한 이름. 이름을 정하지 않은 코스 기록은 null 이고, 응답 조립 시 courseName 으로 대체된다. */
    String getRecordName();

    String getPhotoReportImageUrl();

    String getCliveImageUrl();

    Double getDistance();

    Integer getDuration();

    LocalDateTime getHikedAt();
}
