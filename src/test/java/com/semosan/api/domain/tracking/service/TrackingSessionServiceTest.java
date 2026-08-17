package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.common.weather.WeatherService;
import com.semosan.api.domain.hiking.entity.HikingMember;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.policy.DefaultRecordNameGenerator;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.tracking.dto.request.CreateTrackingSessionRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingRestoreResponse;
import com.semosan.api.domain.tracking.dto.response.TrackingSessionResponse;
import com.semosan.api.domain.tracking.dto.response.TrackingTrackResponse;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.event.TrackingSessionTerminatedEvent;
import com.semosan.api.domain.tracking.repository.TrackingPointRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import com.semosan.api.domain.tracking.repository.projection.TrackingPathProjection;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.hibernate.exception.ConstraintViolationException;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingSessionServiceTest {

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @Mock
    private MountainRepository mountainRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private TrackingSessionStatsService statsService;

    @Mock
    private HikingRecordRepository hikingRecordRepository;

    @Mock
    private HikingMemberRepository hikingMemberRepository;

    @Mock
    private TrackingMilestoneTriggerService milestoneTriggerService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private WeatherService weatherService;

    @Mock
    private DefaultRecordNameGenerator defaultRecordNameGenerator;

    @Mock
    private TrackingPointRepository trackingPointRepository;

    @InjectMocks
    private TrackingSessionService trackingSessionService;

    @Test
    void createSavesFreeRecordingSessionAndInitializesMilestones() {
        User user = user(1L);
        Mountain mountain = mountain(10L);
        CreateTrackingSessionRequest request = new CreateTrackingSessionRequest(10L, 99L, true);
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(trackingSessionRepository.save(any(TrackingSession.class))).thenAnswer(invocation -> {
            TrackingSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 100L);
            return session;
        });

        TrackingSessionResponse response = trackingSessionService.create(1L, request);

        assertThat(response.sessionId()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.mountainId()).isEqualTo(10L);
        assertThat(response.courseId()).isNull();
        assertThat(response.isFreeRecording()).isTrue();
        verify(milestoneTriggerService).initializeMilestones(any(TrackingSession.class));
    }

    @Test
    void createSavesCourseSession() {
        User user = user(1L);
        Mountain mountain = mountain(10L);
        Course course = course(20L, mountain);
        CreateTrackingSessionRequest request = new CreateTrackingSessionRequest(10L, 20L, false);
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(course));
        when(trackingSessionRepository.save(any(TrackingSession.class))).thenAnswer(invocation -> {
            TrackingSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 100L);
            return session;
        });

        TrackingSessionResponse response = trackingSessionService.create(1L, request);

        assertThat(response.courseId()).isEqualTo(20L);
        assertThat(response.isFreeRecording()).isFalse();
        verify(milestoneTriggerService).initializeMilestones(any(TrackingSession.class));
    }

    @Test
    void createThrowsWhenMountainNotFound() {
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(mountainRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingSessionService.create(1L, new CreateTrackingSessionRequest(10L, null, true)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void createThrowsWhenActiveSessionAlreadyExists() {
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(true);

        assertThatThrownBy(() -> trackingSessionService.create(1L, new CreateTrackingSessionRequest(10L, null, true)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_ALREADY_IN_PROGRESS);
    }

    @Test
    void createThrowsWhenCourseIdRequired() {
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain(10L)));

        assertThatThrownBy(() -> trackingSessionService.create(1L, new CreateTrackingSessionRequest(10L, null, false)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_COURSE_ID_REQUIRED);
    }

    @Test
    void createThrowsWhenCourseBelongsToOtherMountain() {
        Mountain mountain = mountain(10L);
        Course course = course(20L, mountain(11L));
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> trackingSessionService.create(1L, new CreateTrackingSessionRequest(10L, 20L, false)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_COURSE_MOUNTAIN_MISMATCH);
    }

    @Test
    void createThrowsWhenCourseNotFound() {
        Mountain mountain = mountain(10L);
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(courseRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingSessionService.create(1L, new CreateTrackingSessionRequest(10L, 20L, false)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COURSE_NOT_FOUND);
    }

    @Test
    void createThrowsWhenCourseHasNoMountain() {
        Mountain mountain = mountain(10L);
        Course course = course(20L, null);
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> trackingSessionService.create(1L, new CreateTrackingSessionRequest(10L, 20L, false)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_COURSE_MOUNTAIN_MISMATCH);
    }

    @Test
    void createConvertsConcurrentActiveSessionUniqueViolation() {
        User user = user(1L);
        Mountain mountain = mountain(10L);
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(trackingSessionRepository.save(any(TrackingSession.class))).thenThrow(activeSessionUniqueViolation());

        assertThatThrownBy(() -> trackingSessionService.create(1L, new CreateTrackingSessionRequest(10L, null, true)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_ALREADY_IN_PROGRESS);
    }

    @Test
    void createRethrowsUnrelatedDataIntegrityViolation() {
        User user = user(1L);
        Mountain mountain = mountain(10L);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("other");
        when(trackingSessionRepository.existsByUser_IdAndStatusIn(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(false);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(trackingSessionRepository.save(any(TrackingSession.class))).thenThrow(exception);

        assertThatThrownBy(() -> trackingSessionService.create(1L, new CreateTrackingSessionRequest(10L, null, true)))
                .isSameAs(exception);
    }

    @Test
    void getActiveReturnsMappedSessionWhenPresent() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findFirstActiveWithRelations(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(Optional.of(session));

        Optional<TrackingSessionResponse> response = trackingSessionService.getActive(1L);

        assertThat(response).isPresent();
        assertThat(response.get().sessionId()).isEqualTo(100L);
    }

    @Test
    void getActiveReturnsEmptyWhenNoActiveSession() {
        when(trackingSessionRepository.findFirstActiveWithRelations(1L, TrackingSessionStatus.ACTIVE_STATES))
                .thenReturn(Optional.empty());

        Optional<TrackingSessionResponse> response = trackingSessionService.getActive(1L);

        assertThat(response).isEmpty();
    }

    @Test
    void getReturnsOwnedSession() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));

        TrackingSessionResponse response = trackingSessionService.get(1L, 100L);

        assertThat(response.sessionId()).isEqualTo(100L);
    }

    @Test
    void pauseAndResumeChangeSessionStatus() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));

        TrackingSessionResponse paused = trackingSessionService.pause(1L, 100L);
        TrackingSessionResponse resumed = trackingSessionService.resume(1L, 100L);

        assertThat(paused.status()).isEqualTo(TrackingSessionStatus.PAUSED);
        assertThat(resumed.status()).isEqualTo(TrackingSessionStatus.IN_PROGRESS);
    }

    @Test
    void completeCreatesHikingRecordMemberAndPublishesTerminationEvent() {
        TrackingSession session = session(100L, user(1L), mountain(10L), course(20L, mountain(10L)), false);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        when(statsService.getStats(100L)).thenReturn(new TrackingSessionStatsService.Stats(1200.0, 100.0, 80.0, 650.0, 5));
        when(weatherService.getTemperature(37.5, 127.0)).thenReturn(Optional.of(18.5));
        when(hikingRecordRepository.save(any(HikingRecord.class))).thenAnswer(invocation -> {
            HikingRecord record = invocation.getArgument(0);
            ReflectionTestUtils.setField(record, "id", 200L);
            return record;
        });

        TrackingSessionResponse response = trackingSessionService.complete(1L, 100L, null);

        assertThat(response.status()).isEqualTo(TrackingSessionStatus.COMPLETED);
        assertThat(response.hikingRecordId()).isEqualTo(200L);
        ArgumentCaptor<HikingRecord> recordCaptor = ArgumentCaptor.forClass(HikingRecord.class);
        verify(hikingRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getDistance()).isEqualTo(1200.0);
        assertThat(recordCaptor.getValue().getTemperature()).isEqualTo(18.5);
        ArgumentCaptor<HikingMember> memberCaptor = ArgumentCaptor.forClass(HikingMember.class);
        verify(hikingMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUser()).isSameAs(session.getUser());
        verify(eventPublisher).publishEvent(any(TrackingSessionTerminatedEvent.class));
    }

    @Test
    void abandonMarksSessionAndPublishesTerminationEvent() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));

        TrackingSessionResponse response = trackingSessionService.abandon(1L, 100L);

        assertThat(response.status()).isEqualTo(TrackingSessionStatus.ABANDONED);
        verify(eventPublisher).publishEvent(any(TrackingSessionTerminatedEvent.class));
    }

    @Test
    void getThrowsWhenSessionOwnedByOtherUser() {
        when(trackingSessionRepository.findByIdWithRelations(100L))
                .thenReturn(Optional.of(session(100L, user(2L), mountain(10L), null, true)));

        assertThatThrownBy(() -> trackingSessionService.get(1L, 100L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
    }

    @Test
    void getThrowsWhenSessionNotFound() {
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingSessionService.get(1L, 100L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_NOT_FOUND);
    }

    @Test
    void completeStoresRequestedNameForFreeRecording() {
        TrackingSession session = completableSession(null);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        stubCompleteDependencies();

        trackingSessionService.complete(1L, 100L, "  북한산 아침 산책  ");

        // 앞뒤 공백은 제거하고 저장한다.
        assertThat(savedRecord().getName()).isEqualTo("북한산 아침 산책");
        verify(hikingRecordRepository, never()).countFreeRecordsByUserAndDay(any(), any(), any());
    }

    @Test
    void completeGeneratesDefaultNameWhenFreeRecordingNameIsBlank() {
        TrackingSession session = completableSession(null);
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.of(2026, 7, 23, 9, 30));
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        stubCompleteDependencies();
        // 같은 날 이미 2건 있었으므로 이번은 3번째다.
        when(hikingRecordRepository.countFreeRecordsByUserAndDay(
                1L,
                LocalDateTime.of(2026, 7, 23, 0, 0),
                LocalDateTime.of(2026, 7, 24, 0, 0)))
                .thenReturn(2L);
        when(defaultRecordNameGenerator.generate(any(), any(), eq(3L))).thenReturn("260723_등산왕의코스3");

        trackingSessionService.complete(1L, 100L, "   ");

        assertThat(savedRecord().getName()).isEqualTo("260723_등산왕의코스3");
    }

    @Test
    void completeIgnoresNameForCourseRecording() {
        Mountain mountain = mountain(10L);
        TrackingSession session = completableSession(course(20L, mountain));
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        stubCompleteDependencies();

        trackingSessionService.complete(1L, 100L, "무시되어야 하는 이름");

        // 코스 기록은 courses.name 으로 표시되므로 별도 이름을 두지 않는다.
        assertThat(savedRecord().getName()).isNull();
        verify(hikingRecordRepository, never()).countFreeRecordsByUserAndDay(any(), any(), any());
    }

    private TrackingSession completableSession(Course course) {
        TrackingSession session = session(100L, user(1L), mountain(10L), course, course == null);
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.of(2026, 7, 23, 9, 30));
        return session;
    }

    private void stubCompleteDependencies() {
        when(statsService.getStats(100L))
                .thenReturn(new TrackingSessionStatsService.Stats(1200.0, 100.0, 80.0, 650.0, 5));
        when(hikingRecordRepository.save(any(HikingRecord.class))).thenAnswer(invocation -> {
            HikingRecord record = invocation.getArgument(0);
            ReflectionTestUtils.setField(record, "id", 200L);
            return record;
        });
    }

    private HikingRecord savedRecord() {
        ArgumentCaptor<HikingRecord> captor = ArgumentCaptor.forClass(HikingRecord.class);
        verify(hikingRecordRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void getTrackReturnsGeoJsonPath() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        TrackingPathProjection projection = mock(TrackingPathProjection.class);
        when(projection.getTrack()).thenReturn("{\"type\":\"LineString\",\"coordinates\":[[127.0,37.5],[127.1,37.6]]}");
        when(projection.getAltitudes()).thenReturn("[312.0, 315.5]");
        when(trackingPointRepository.findTrackBySessionId(100L)).thenReturn(Optional.of(projection));

        TrackingTrackResponse response = trackingSessionService.getTrack(1L, 100L);

        assertThat(response.sessionId()).isEqualTo(100L);
        assertThat(response.track()).contains("LineString");
        assertThat(response.altitudes()).isEqualTo("[312.0, 315.5]");
    }

    @Test
    void getTrackReturnsEmptyWhenLineStringIsNull() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        TrackingPathProjection projection = mock(TrackingPathProjection.class);
        // 점이 0~1개면 ST_MakeLine 이 null 을 반환한다.
        when(projection.getTrack()).thenReturn(null);
        when(trackingPointRepository.findTrackBySessionId(100L)).thenReturn(Optional.of(projection));

        TrackingTrackResponse response = trackingSessionService.getTrack(1L, 100L);

        assertThat(response.sessionId()).isEqualTo(100L);
        assertThat(response.track()).isNull();
        assertThat(response.altitudes()).isNull();
    }

    @Test
    void getTrackReturnsEmptyWhenProjectionMissing() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        when(trackingPointRepository.findTrackBySessionId(100L)).thenReturn(Optional.empty());

        TrackingTrackResponse response = trackingSessionService.getTrack(1L, 100L);

        assertThat(response.track()).isNull();
    }

    @Test
    void getTrackThrowsWhenSessionNotOwned() {
        when(trackingSessionRepository.findByIdWithRelations(100L))
                .thenReturn(Optional.of(session(100L, user(2L), mountain(10L), null, true)));

        assertThatThrownBy(() -> trackingSessionService.getTrack(1L, 100L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
    }

    @Test
    void restoreReturnsStatsAndMilestoneState() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        when(statsService.getStats(100L))
                .thenReturn(new TrackingSessionStatsService.Stats(3241.7, 452.0, 88.3, 781.2, 842L));
        when(milestoneTriggerService.getMilestoneState(100L)).thenReturn(
                new TrackingMilestoneTriggerService.MilestoneState(
                        List.of(812.5, 1625.0), List.of(0, 1), List.of(0), true)
        );

        TrackingRestoreResponse response = trackingSessionService.restore(1L, 100L);

        assertThat(response.session().sessionId()).isEqualTo(100L);
        assertThat(response.elapsedSeconds()).isPositive();
        assertThat(response.stats().distanceMeters()).isEqualTo(3241.7);
        assertThat(response.stats().maxAltitudeMeters()).isEqualTo(781.2);
        assertThat(response.stats().pointCount()).isEqualTo(842L);
        assertThat(response.photoMilestone().openedIndexes()).containsExactly(0, 1);
        assertThat(response.photoMilestone().closedIndexes()).containsExactly(0);
        assertThat(response.photoMilestone().summitNotified()).isTrue();
    }

    @Test
    void restoreReturnsNullStatsWhenRedisKeyExpired() {
        TrackingSession session = session(100L, user(1L), mountain(10L), null, true);
        when(trackingSessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        // TTL 24h 만료 또는 GPS 점이 한 건도 없던 경우 — 전 필드가 0 으로 나온다.
        when(statsService.getStats(100L))
                .thenReturn(new TrackingSessionStatsService.Stats(0.0, 0.0, 0.0, null, 0L));
        when(milestoneTriggerService.getMilestoneState(100L)).thenReturn(
                new TrackingMilestoneTriggerService.MilestoneState(List.of(), List.of(), List.of(), false)
        );

        TrackingRestoreResponse response = trackingSessionService.restore(1L, 100L);

        assertThat(response.stats()).isNull();
        assertThat(response.photoMilestone().milestones()).isEmpty();
    }

    @Test
    void restoreThrowsWhenSessionNotOwned() {
        when(trackingSessionRepository.findByIdWithRelations(100L))
                .thenReturn(Optional.of(session(100L, user(2L), mountain(10L), null, true)));

        assertThatThrownBy(() -> trackingSessionService.restore(1L, 100L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
    }

    private DataIntegrityViolationException activeSessionUniqueViolation() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "duplicate active session",
                new SQLException("unique violation"),
                "uq_tracking_sessions_user_active"
        );
        return new DataIntegrityViolationException("duplicate active session", cause);
    }

    private TrackingSession session(Long id, User user, Mountain mountain, Course course, boolean freeRecording) {
        TrackingSession session = TrackingSession.create(user, mountain, course, freeRecording);
        ReflectionTestUtils.setField(session, "id", id);
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(session, "pausedSecondsTotal", 0);
        return session;
    }

    private User user(Long id) {
        User user = User.createTestUser("test-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "weight", 70.0);
        return user;
    }

    private Mountain mountain(Long id) {
        Mountain mountain = newInstance(Mountain.class);
        ReflectionTestUtils.setField(mountain, "id", id);
        ReflectionTestUtils.setField(mountain, "name", "관악산");
        ReflectionTestUtils.setField(mountain, "latitude", 37.5);
        ReflectionTestUtils.setField(mountain, "longitude", 127.0);
        return mountain;
    }

    private Course course(Long id, Mountain mountain) {
        Course course = newInstance(Course.class);
        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "mountain", mountain);
        ReflectionTestUtils.setField(course, "name", "정상 코스");
        ReflectionTestUtils.setField(course, "difficulty", Difficulty.NORMAL);
        ReflectionTestUtils.setField(course, "distance", 1500.0);
        ReflectionTestUtils.setField(course, "duration", 90);
        return course;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
