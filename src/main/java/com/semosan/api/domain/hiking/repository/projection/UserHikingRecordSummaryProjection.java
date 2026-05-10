package com.semosan.api.domain.hiking.repository.projection;

public interface UserHikingRecordSummaryProjection {

    Long getTotalHikingCount();

    Long getConqueredMountainCount();

    Double getTotalAltitude();
}
