package com.semosan.api.domain.mountain.repository.projection;

public interface MountainMapProjection {

    Long getId();

    String getName();

    Double getLatitude();

    Double getLongitude();

    Long getVisitCount();

    String getImageUrl();
}
