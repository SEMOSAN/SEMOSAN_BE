package com.semosan.api.domain.tracking.dto.response;

import com.semosan.api.domain.tracking.entity.TrackingPhoto;

import java.time.LocalDateTime;

public record TrackingPhotoResponse(
        Long photoId,
        Long trackingSessionId,
        Integer milestoneIndex,
        Double milestoneDistanceM,
        String imageUrl,
        LocalDateTime capturedAt,
        Double lat,
        Double lng,
        Double altitude
) {

    public static TrackingPhotoResponse from(TrackingPhoto photo) {
        return new TrackingPhotoResponse(
                photo.getId(),
                photo.getTrackingSession().getId(),
                photo.getMilestoneIndex(),
                photo.getMilestoneDistanceM(),
                photo.getImageUrl(),
                photo.getCapturedAt(),
                photo.getLat(),
                photo.getLng(),
                photo.getAltitude()
        );
    }
}
