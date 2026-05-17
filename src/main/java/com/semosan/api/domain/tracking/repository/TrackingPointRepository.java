package com.semosan.api.domain.tracking.repository;

import com.semosan.api.domain.tracking.entity.TrackingPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingPointRepository extends JpaRepository<TrackingPoint, Long> {

    List<TrackingPoint> findByTrackingSession_IdOrderByRecordedAtAsc(Long sessionId);
}
