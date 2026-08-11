package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.hiking.entity.HikingMember;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.policy.DefaultRecordNameGenerator;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.tracking.dto.request.CreateTrackingSessionRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingSessionResponse;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.event.TrackingSessionTerminatedEvent;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import com.semosan.api.common.exception.ConstraintViolationUtils;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import com.semosan.api.common.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingSessionService {

    private static final String ACTIVE_SESSION_UNIQUE_INDEX = "uq_tracking_sessions_user_active";

    private final TrackingSessionRepository trackingSessionRepository;
    private final MountainRepository mountainRepository;
    private final CourseRepository courseRepository;
    private final UserReader userReader;
    private final TrackingSessionStatsService statsService;
    private final HikingRecordRepository hikingRecordRepository;
    private final HikingMemberRepository hikingMemberRepository;
    private final TrackingMilestoneTriggerService milestoneTriggerService;
    private final ApplicationEventPublisher eventPublisher;
    private final WeatherService weatherService;
    private final DefaultRecordNameGenerator defaultRecordNameGenerator;

    /**
     * 세션 생성. 유저당 진행 중 세션은 1개만 허용한다.
     *
     * TODO/주의 — 사용자가 종료(complete/abandon)를 누르지 않고 앱을 강제 종료하면 이전 세션이 IN_PROGRESS
     * 로 유령처럼 남아 다음 세션 생성을 막을 수 있다. 대응 메커니즘:
     *  1) 클라이언트 진입 시 GET /me/active 로 진행 중 세션 확인 후 "이어서 / 폐기 후 새로 시작" 분기 노출
     *  2) {@link TrackingSessionExpiryScheduler} 가 24h 무업데이트 세션을 ABANDONED 처리하는 fallback
     * 운영 시 자주 발생할 수 있는 케이스이므로 클라이언트 흐름에서 (1)을 반드시 구현할 것.
     */
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
        TrackingSession saved = saveSession(session);
        log.info("[SESSION] 세션 생성 | sessionId={} | userId={} | mountainId={}", saved.getId(), userId, request.mountainId());

        // 세션 생성과 동시에 사진 마일스톤 거리 리스트(코스 4컷 / 자유 6컷)를 Redis 에 적재한다.
        // 이후 GPS Consumer 가 매 점마다 evaluate 호출 시 이 키를 읽어 마일스톤 도달을 판정하므로,
        // 여기서 빠지면 모든 photo-window OPEN/CLOSED 와 FCM 알림이 영영 발송되지 않는다.
        milestoneTriggerService.initializeMilestones(saved);

        return TrackingSessionResponse.from(saved);
    }

    public Optional<TrackingSessionResponse> getActive(Long userId) {
        return trackingSessionRepository
                .findFirstActiveWithRelations(userId, TrackingSessionStatus.ACTIVE_STATES)
                .map(TrackingSessionResponse::from);
    }

    public TrackingSessionResponse get(Long userId, Long sessionId) {
        return TrackingSessionResponse.from(findOwnedSession(userId, sessionId));
    }

    @Transactional
    public TrackingSessionResponse pause(Long userId, Long sessionId) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        session.pause();
        log.info("[SESSION] 세션 일시정지 | sessionId={} | userId={}", sessionId, userId);
        return TrackingSessionResponse.from(session);
    }

    @Transactional
    public TrackingSessionResponse resume(Long userId, Long sessionId) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        session.resume();
        log.info("[SESSION] 세션 재개 | sessionId={} | userId={}", sessionId, userId);
        return TrackingSessionResponse.from(session);
    }

    /**
     * 정상 종료. 세션 상태를 COMPLETED 로 마감하고, Redis Hash 의 실시간 통계를 스냅샷 떠
     * HikingRecord + HikingMember 로 영구 변환한다.
     *  - 통계는 GPS Consumer 가 메시지 수신 즉시 갱신해두므로 종료 시점의 스냅샷이 최신값.
     *  - 동행자 기능 미구현이므로 본인 1명만 HikingMember 로 등록.
     *  - cliveImageUrl / photoReportImageUrl 은 #46 사진 흐름에서 채워질 예정 (현재 null).
     */
    @Transactional
    public TrackingSessionResponse complete(Long userId, Long sessionId, String requestedName) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        session.complete();

        TrackingSessionStatsService.Stats stats = statsService.getStats(sessionId);
        if (stats.pointCount() == 0) {
            // GPS 점이 한 건도 들어오지 않은 채 종료 — 사용자의 명시적 종료는 존중하되 통계가 비어있음을 알림.
            log.warn("Completing tracking session {} with no GPS points; stats will be zero/null", sessionId);
        }
        HikingRecord record = HikingRecord.fromTrackingSession(
                session,
                stats.distanceMeters(),
                stats.maxAltitudeMeters(),
                stats.ascentMeters(),
                stats.descentMeters(),
                resolveRecordName(session, requestedName)
        );
        Mountain mountain = session.getMountain();
        if (mountain.getLatitude() != null && mountain.getLongitude() != null) {
            weatherService.getTemperature(mountain.getLatitude(), mountain.getLongitude())
                    .ifPresent(record::updateTemperature);
        }

        HikingRecord savedRecord = hikingRecordRepository.save(record);

        HikingMember member = HikingMember.create(savedRecord, session.getUser());
        hikingMemberRepository.save(member);

        eventPublisher.publishEvent(new TrackingSessionTerminatedEvent(sessionId));
        log.info("[SESSION] 세션 완료 | sessionId={} | userId={} | distance={}m | altitude={}m",
                sessionId, userId, stats.distanceMeters(), stats.maxAltitudeMeters());

        return TrackingSessionResponse.from(session, savedRecord.getId());
    }

    @Transactional
    public TrackingSessionResponse abandon(Long userId, Long sessionId) {
        TrackingSession session = findOwnedSession(userId, sessionId);
        session.abandon();
        eventPublisher.publishEvent(new TrackingSessionTerminatedEvent(sessionId));
        log.info("[SESSION] 세션 포기 | sessionId={} | userId={}", sessionId, userId);
        return TrackingSessionResponse.from(session);
    }

    /**
     * 기록에 남길 이름을 정한다.
     *
     *  - 코스 기록: courses.name 으로 표시되므로 이름을 두지 않는다. 값이 와도 무시한다
     *    (자유기록에서 courseId 를 무시하는 것과 같은 방향).
     *  - 자유기록 + 입력값 있음: 앞뒤 공백만 제거해 그대로 쓴다.
     *  - 자유기록 + 입력값 없음: 같은 날 순번을 세어 기본 이름을 만든다.
     */
    private String resolveRecordName(TrackingSession session, String requestedName) {
        if (session.getCourse() != null) {
            return null;
        }
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        LocalDateTime hikedAt = session.getStartedAt();
        LocalDateTime dayStart = hikedAt.toLocalDate().atStartOfDay();
        long sequence = hikingRecordRepository.countFreeRecordsByUserAndDay(
                session.getUser().getId(), dayStart, dayStart.plusDays(1)) + 1;
        return defaultRecordNameGenerator.generate(session.getUser(), hikedAt, sequence);
    }

    private TrackingSession findOwnedSession(Long userId, Long sessionId) {
        TrackingSession session = trackingSessionRepository.findByIdWithRelations(sessionId)
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

    private TrackingSession saveSession(TrackingSession session) {
        try {
            return trackingSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 유니크 제약 위반 시 이미 진행 중인 세션 존재 에러로 변환
            if (ConstraintViolationUtils.isViolation(e, ACTIVE_SESSION_UNIQUE_INDEX)) {
                throw new GeneralException(ErrorStatus.TRACKING_SESSION_ALREADY_IN_PROGRESS);
            }
            throw e;
        }
    }
}
