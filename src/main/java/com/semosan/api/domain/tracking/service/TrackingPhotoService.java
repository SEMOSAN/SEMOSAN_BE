package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.tracking.dto.request.TrackingPhotoUploadRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingPhotoResponse;
import com.semosan.api.domain.tracking.entity.TrackingPhoto;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.repository.TrackingPhotoRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingPhotoService {

    private static final Set<TrackingSessionStatus> ACTIVE_STATES =
            EnumSet.of(TrackingSessionStatus.IN_PROGRESS, TrackingSessionStatus.PAUSED);

    private final TrackingPhotoRepository trackingPhotoRepository;
    private final TrackingSessionRepository trackingSessionRepository;

    @Transactional
    public TrackingPhotoResponse upload(Long userId, Long sessionId, TrackingPhotoUploadRequest request) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        if (!ACTIVE_STATES.contains(session.getStatus())) {
            throw new GeneralException(ErrorStatus.TRACKING_PHOTO_SESSION_INACTIVE);
        }
        if (trackingPhotoRepository.existsByTrackingSession_IdAndMilestoneIndex(sessionId, request.milestoneIndex())) {
            throw new GeneralException(ErrorStatus.TRACKING_PHOTO_DUPLICATE);
        }
        TrackingPhoto photo = TrackingPhoto.create(
                session,
                request.milestoneIndex(),
                request.milestoneDistanceM(),
                request.imageUrl(),
                request.capturedAt(),
                request.lat(),
                request.lng(),
                request.altitude()
        );
        TrackingPhoto saved = trackingPhotoRepository.save(photo);
        return TrackingPhotoResponse.from(saved);
    }

    public List<TrackingPhotoResponse> listBySession(Long userId, Long sessionId) {
        findOwnedSession(userId, sessionId);
        return trackingPhotoRepository.findByTrackingSession_IdOrderByMilestoneIndexAsc(sessionId).stream()
                .map(TrackingPhotoResponse::from)
                .toList();
    }

    private TrackingSession findOwnedSession(Long userId, Long sessionId) {
        TrackingSession session = trackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TRACKING_SESSION_NOT_FOUND));
        if (!session.isOwnedBy(userId)) {
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
        }
        return session;
    }
}
