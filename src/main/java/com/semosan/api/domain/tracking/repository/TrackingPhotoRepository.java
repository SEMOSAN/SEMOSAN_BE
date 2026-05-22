package com.semosan.api.domain.tracking.repository;

import com.semosan.api.domain.tracking.entity.TrackingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingPhotoRepository extends JpaRepository<TrackingPhoto, Long> {

    List<TrackingPhoto> findByTrackingSession_IdOrderByMilestoneIndexAsc(Long trackingSessionId);

    boolean existsByTrackingSession_IdAndMilestoneIndex(Long trackingSessionId, Integer milestoneIndex);
}
