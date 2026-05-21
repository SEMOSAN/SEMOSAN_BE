package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.tracking.dto.request.CreateTrackingSessionRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingSessionResponse;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingSessionService {

    private final TrackingSessionRepository trackingSessionRepository;
    private final MountainRepository mountainRepository;
    private final CourseRepository courseRepository;
    private final UserReader userReader;

    @Transactional
    public TrackingSessionResponse create(Long userId, CreateTrackingSessionRequest request) {
        if (trackingSessionRepository.existsByUser_IdAndStatusIn(userId, TrackingSessionStatus.ACTIVE_STATES)) {
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_ALREADY_IN_PROGRESS);
        }
        User user = userReader.findActiveUserById(userId);
        Mountain mountain = mountainRepository.findById(request.mountainId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND));

        Course course = resolveCourse(request, mountain);

        TrackingSession session = TrackingSession.create(user, mountain, course, request.isFreeRecording());
        TrackingSession saved = trackingSessionRepository.save(session);
        return TrackingSessionResponse.from(saved);
    }

    public Optional<TrackingSessionResponse> getActive(Long userId) {
        return trackingSessionRepository
                .findFirstByUser_IdAndStatusInOrderByStartedAtDesc(userId, TrackingSessionStatus.ACTIVE_STATES)
                .map(TrackingSessionResponse::from);
    }

    public TrackingSessionResponse get(Long userId, Long sessionId) {
        return TrackingSessionResponse.from(findOwnedSession(userId, sessionId));
    }

    @Transactional
    public TrackingSessionResponse pause(Long userId, Long sessionId) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        session.pause();
        return TrackingSessionResponse.from(session);
    }

    @Transactional
    public TrackingSessionResponse resume(Long userId, Long sessionId) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        session.resume();
        return TrackingSessionResponse.from(session);
    }

    /**
     * 정상 종료. 본 메서드는 상태/시각만 마감하고, 실제 HikingRecord 변환은 #20 에서 추가될 예정.
     * TODO(#20): 종료 시 Redis Stream 의 GPS 점들을 모아 통계 계산 + HikingRecord/HikingMember 생성.
     */
    @Transactional
    public TrackingSessionResponse complete(Long userId, Long sessionId) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        session.complete();
        return TrackingSessionResponse.from(session);
    }

    @Transactional
    public TrackingSessionResponse abandon(Long userId, Long sessionId) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        session.abandon();
        return TrackingSessionResponse.from(session);
    }

    private TrackingSession findOwnedSession(Long userId, Long sessionId) {
        TrackingSession session = trackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TRACKING_SESSION_NOT_FOUND));
        if (!session.isOwnedBy(userId)) {
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
        }
        return session;
    }

    private Course resolveCourse(CreateTrackingSessionRequest request, Mountain mountain) {
        if (Boolean.TRUE.equals(request.isFreeRecording())) {
            // 자유 기록은 코스 미선택. courseId 가 와도 무시.
            return null;
        }
        if (request.courseId() == null) {
            throw new GeneralException(ErrorStatus.TRACKING_COURSE_ID_REQUIRED);
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
        if (course.getMountain() == null || !course.getMountain().getId().equals(mountain.getId())) {
            throw new GeneralException(ErrorStatus.TRACKING_COURSE_MOUNTAIN_MISMATCH);
        }
        return course;
    }
}
