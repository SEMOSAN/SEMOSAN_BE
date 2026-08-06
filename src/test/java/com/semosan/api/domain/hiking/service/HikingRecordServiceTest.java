package com.semosan.api.domain.hiking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordSummaryResponse;
import com.semosan.api.domain.hiking.dto.response.HikingRecordDetailResponse;
import com.semosan.api.domain.hiking.dto.request.CreateCourseDifficultyFeedbackRequest;
import com.semosan.api.domain.hiking.dto.response.CourseDifficultyFeedbackResponse;
import com.semosan.api.domain.hiking.entity.CourseDifficultyFeedback;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.enums.DifficultyFeedbackType;
import com.semosan.api.domain.hiking.repository.CourseDifficultyFeedbackRepository;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.tracking.entity.TrackingPhoto;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.repository.TrackingPhotoRepository;
import com.semosan.api.domain.tracking.repository.TrackingPointRepository;
import com.semosan.api.domain.tracking.repository.projection.TrackingPathProjection;
import com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.Gender;
import com.semosan.api.domain.user.service.UserReader;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HikingRecordServiceTest {

    @Mock
    private HikingRecordRepository hikingRecordRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private MountainRepository mountainRepository;

    @Mock
    private HikingMemberRepository hikingMemberRepository;

    @Mock
    private CourseDifficultyFeedbackRepository courseDifficultyFeedbackRepository;

    @Mock
    private TrackingPointRepository trackingPointRepository;

    @Mock
    private TrackingPhotoRepository trackingPhotoRepository;

    @InjectMocks
    private HikingRecordService hikingRecordService;

    @Test
    void getUserHikingRecordsByMountainIdThrowsWhenMountainMissing() {
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(mountainRepository.existsById(20L)).thenReturn(false);

        assertThatThrownBy(() -> hikingRecordService.getUserHikingRecordsByMountainId(
                1L,
                20L,
                org.springframework.data.domain.PageRequest.of(0, 10)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void getUserHikingRecordSummaryReturnsEmptyWhenProjectionMissing() {
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(hikingRecordRepository.findUserHikingRecordSummaryByUserId(1L)).thenReturn(null);

        GetUserHikingRecordSummaryResponse response = hikingRecordService.getUserHikingRecordSummary(1L);

        assertThat(response.totalHikingCount()).isZero();
        assertThat(response.conqueredMountainCount()).isZero();
        assertThat(response.totalAltitude()).isZero();
    }

    @Test
    void getUserHikingRecordSummaryMapsProjection() {
        var projection = mock(com.semosan.api.domain.hiking.repository.projection.UserHikingRecordSummaryProjection.class);
        when(projection.getTotalHikingCount()).thenReturn(3L);
        when(projection.getConqueredMountainCount()).thenReturn(2L);
        when(projection.getTotalAltitude()).thenReturn(1500.5);
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(hikingRecordRepository.findUserHikingRecordSummaryByUserId(1L)).thenReturn(projection);

        GetUserHikingRecordSummaryResponse response = hikingRecordService.getUserHikingRecordSummary(1L);

        assertThat(response.totalHikingCount()).isEqualTo(3L);
        assertThat(response.conqueredMountainCount()).isEqualTo(2L);
        assertThat(response.totalAltitude()).isEqualTo(1500.5);
    }

    @Test
    void getHikingRecordDetailReturnsTrackAndPhotosForOwnedTrackingRecord() throws Exception {
        User user = user(1L);
        Course course = course(mountain(20L, "관악산"), 30L, "정상 코스");
        HikingRecord hikingRecord = hikingRecord(10L, course);
        TrackingSession session = trackingSession(100L);
        ReflectionTestUtils.setField(hikingRecord, "trackingSession", session);
        ReflectionTestUtils.setField(hikingRecord, "duration", 3600);
        ReflectionTestUtils.setField(hikingRecord, "distance", 1200.0);
        ReflectionTestUtils.setField(hikingRecord, "maxAltitude", 650.0);
        ReflectionTestUtils.setField(hikingRecord, "ascent", 100.0);
        ReflectionTestUtils.setField(hikingRecord, "descent", 80.0);
        ReflectionTestUtils.setField(hikingRecord, "calories", 400);
        ReflectionTestUtils.setField(hikingRecord, "temperature", 18.5);
        ReflectionTestUtils.setField(hikingRecord, "startedAt", LocalDateTime.of(2026, 8, 6, 10, 0));
        ReflectionTestUtils.setField(hikingRecord, "endedAt", LocalDateTime.of(2026, 8, 6, 11, 0));
        TrackingPathProjection path = mock(TrackingPathProjection.class);
        when(path.getTrack()).thenReturn("{\"type\":\"LineString\"}");
        when(path.getAltitudes()).thenReturn("[100,120]");
        TrackingPhoto photo = TrackingPhoto.create(session, 0, 500.0,
                "photo.jpg", LocalDateTime.of(2026, 8, 6, 10, 30), 37.5, 127.0, 123.4);

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(hikingRecordRepository.findWithMountainAndCourseById(10L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)).thenReturn(true);
        when(trackingPointRepository.findTrackBySessionId(100L)).thenReturn(Optional.of(path));
        when(trackingPhotoRepository.findByTrackingSession_IdOrderByMilestoneIndexAsc(100L)).thenReturn(List.of(photo));

        HikingRecordDetailResponse response = hikingRecordService.getHikingRecordDetail(1L, 10L);

        assertThat(response.hikingRecordId()).isEqualTo(10L);
        assertThat(response.mountain().id()).isEqualTo(20L);
        assertThat(response.course().id()).isEqualTo(30L);
        assertThat(response.track()).isEqualTo("{\"type\":\"LineString\"}");
        assertThat(response.altitudes()).isEqualTo("[100,120]");
        assertThat(response.photos().getFirst().imageUrl()).isEqualTo("photo.jpg");
    }

    @Test
    void getHikingRecordDetailThrowsWhenRecordMissing() {
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(hikingRecordRepository.findWithMountainAndCourseById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hikingRecordService.getHikingRecordDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.HIKING_RECORD_NOT_FOUND);
    }

    @Test
    void getHikingRecordDetailThrowsWhenRecordNotOwned() throws Exception {
        User user = user(1L);
        HikingRecord hikingRecord = hikingRecord(10L, null);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(hikingRecordRepository.findWithMountainAndCourseById(10L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)).thenReturn(false);

        assertThatThrownBy(() -> hikingRecordService.getHikingRecordDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.HIKING_RECORD_FORBIDDEN);
    }

    @Test
    void createCourseDifficultyFeedbackSavesFeedbackForOwnedCourseRecord() throws Exception {
        User user = user(1L);
        HikingRecord hikingRecord = hikingRecord(10L, course(mountain(20L, "관악산"), 30L, "과천향교 출발 코스"));
        CreateCourseDifficultyFeedbackRequest request =
                new CreateCourseDifficultyFeedbackRequest(DifficultyFeedbackType.HARDER);

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(hikingRecordRepository.findById(10L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)).thenReturn(true);
        when(courseDifficultyFeedbackRepository.existsByHikingRecord_Id(10L)).thenReturn(false);
        when(courseDifficultyFeedbackRepository.saveAndFlush(any(CourseDifficultyFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseDifficultyFeedbackResponse response = hikingRecordService.createCourseDifficultyFeedback(
                1L,
                10L,
                request
        );

        assertThat(response.hikingRecordId()).isEqualTo(10L);
        assertThat(response.mountainId()).isEqualTo(20L);
        assertThat(response.courseId()).isEqualTo(30L);
        assertThat(response.guideDifficulty()).isEqualTo(Difficulty.NORMAL);
        assertThat(response.comparison()).isEqualTo(DifficultyFeedbackType.HARDER);
        verify(courseDifficultyFeedbackRepository).saveAndFlush(any(CourseDifficultyFeedback.class));
    }

    @Test
    void createCourseDifficultyFeedbackThrowsWhenRecordIsNotOwnedByUser() throws Exception {
        User user = user(1L);
        HikingRecord hikingRecord = hikingRecord(10L, course(mountain(20L, "관악산"), 30L, "과천향교 출발 코스"));

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(hikingRecordRepository.findById(10L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)).thenReturn(false);

        assertThatThrownBy(() -> hikingRecordService.createCourseDifficultyFeedback(
                1L,
                10L,
                new CreateCourseDifficultyFeedbackRequest(DifficultyFeedbackType.SIMILAR)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.HIKING_RECORD_FORBIDDEN);
        verify(courseDifficultyFeedbackRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCourseDifficultyFeedbackThrowsWhenRecordHasNoCourse() throws Exception {
        User user = user(1L);
        HikingRecord hikingRecord = hikingRecord(10L, null);

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(hikingRecordRepository.findById(10L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)).thenReturn(true);

        assertThatThrownBy(() -> hikingRecordService.createCourseDifficultyFeedback(
                1L,
                10L,
                new CreateCourseDifficultyFeedbackRequest(DifficultyFeedbackType.EASIER)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.HIKING_RECORD_COURSE_REQUIRED);
        verify(courseDifficultyFeedbackRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCourseDifficultyFeedbackThrowsWhenFeedbackAlreadyExists() throws Exception {
        User user = user(1L);
        HikingRecord hikingRecord = hikingRecord(10L, course(mountain(20L, "관악산"), 30L, "과천향교 출발 코스"));

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(hikingRecordRepository.findById(10L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)).thenReturn(true);
        when(courseDifficultyFeedbackRepository.existsByHikingRecord_Id(10L)).thenReturn(true);

        assertThatThrownBy(() -> hikingRecordService.createCourseDifficultyFeedback(
                1L,
                10L,
                new CreateCourseDifficultyFeedbackRequest(DifficultyFeedbackType.SIMILAR)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COURSE_DIFFICULTY_FEEDBACK_ALREADY_EXISTS);
        verify(courseDifficultyFeedbackRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCourseDifficultyFeedbackThrowsConflictWhenConcurrentDuplicateSaveOccurs() throws Exception {
        User user = user(1L);
        HikingRecord hikingRecord = hikingRecord(10L, course(mountain(20L, "관악산"), 30L, "과천향교 출발 코스"));

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(hikingRecordRepository.findById(10L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)).thenReturn(true);
        when(courseDifficultyFeedbackRepository.existsByHikingRecord_Id(10L)).thenReturn(false);
        when(courseDifficultyFeedbackRepository.saveAndFlush(any(CourseDifficultyFeedback.class)))
                .thenThrow(duplicateFeedbackDataIntegrityViolation());

        assertThatThrownBy(() -> hikingRecordService.createCourseDifficultyFeedback(
                1L,
                10L,
                new CreateCourseDifficultyFeedbackRequest(DifficultyFeedbackType.SIMILAR)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COURSE_DIFFICULTY_FEEDBACK_ALREADY_EXISTS);
    }

    @Test
    void createCourseDifficultyFeedbackRethrowsUnrelatedDataIntegrityViolation() throws Exception {
        User user = user(1L);
        HikingRecord hikingRecord = hikingRecord(10L, course(mountain(20L, "관악산"), 30L, "과천향교 출발 코스"));
        DataIntegrityViolationException exception = new DataIntegrityViolationException("other constraint");

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(hikingRecordRepository.findById(10L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)).thenReturn(true);
        when(courseDifficultyFeedbackRepository.existsByHikingRecord_Id(10L)).thenReturn(false);
        when(courseDifficultyFeedbackRepository.saveAndFlush(any(CourseDifficultyFeedback.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> hikingRecordService.createCourseDifficultyFeedback(
                1L,
                10L,
                new CreateCourseDifficultyFeedbackRequest(DifficultyFeedbackType.SIMILAR)
        ))
                .isSameAs(exception);
    }

    @Test
    void courseDifficultyFeedbackCreateThrowsWhenRecordHasNoCourse() throws Exception {
        HikingRecord hikingRecord = hikingRecord(10L, null);

        assertThatThrownBy(() -> CourseDifficultyFeedback.create(
                hikingRecord,
                user(1L),
                DifficultyFeedbackType.SIMILAR
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.HIKING_RECORD_COURSE_REQUIRED);
    }

    private DataIntegrityViolationException duplicateFeedbackDataIntegrityViolation() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "duplicate hiking_record_id",
                new SQLException("unique violation"),
                "uk_course_difficulty_feedback_hiking_record"
        );
        return new DataIntegrityViolationException("duplicate hiking_record_id", cause);
    }

    private User user(Long id) {
        User user = User.createTestUser("hiking-record-service-test-user", DeviceType.IOS);
        user.completeOnboarding(new CompleteOnboardingCommand(
                "테스트",
                null,
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                175.0,
                70.0
        ));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Mountain mountain(Long id, String name) throws Exception {
        Constructor<Mountain> constructor = Mountain.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Mountain mountain = constructor.newInstance();
        ReflectionTestUtils.setField(mountain, "id", id);
        ReflectionTestUtils.setField(mountain, "name", name);
        return mountain;
    }

    private Course course(Mountain mountain, Long id, String name) throws Exception {
        Constructor<Course> constructor = Course.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Course course = constructor.newInstance();
        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "mountain", mountain);
        ReflectionTestUtils.setField(course, "name", name);
        ReflectionTestUtils.setField(course, "difficulty", Difficulty.NORMAL);
        return course;
    }

    private HikingRecord hikingRecord(Long id, Course course) throws Exception {
        Constructor<HikingRecord> constructor = HikingRecord.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        HikingRecord hikingRecord = constructor.newInstance();
        ReflectionTestUtils.setField(hikingRecord, "id", id);
        ReflectionTestUtils.setField(hikingRecord, "course", course);
        ReflectionTestUtils.setField(hikingRecord, "mountain", course == null ? mountain(20L, "자유기록산") : course.getMountain());
        return hikingRecord;
    }

    private TrackingSession trackingSession(Long id) throws Exception {
        Constructor<TrackingSession> constructor = TrackingSession.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        TrackingSession session = constructor.newInstance();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
