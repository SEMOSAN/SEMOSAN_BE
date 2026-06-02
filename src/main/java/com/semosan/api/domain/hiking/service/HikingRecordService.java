package com.semosan.api.domain.hiking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.hiking.dto.request.CreateCourseDifficultyFeedbackRequest;
import com.semosan.api.domain.hiking.dto.response.CourseDifficultyFeedbackResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingMountainRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordSummaryResponse;
import com.semosan.api.domain.hiking.dto.response.HikingRecordDetailResponse;
import com.semosan.api.domain.hiking.entity.CourseDifficultyFeedback;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.repository.CourseDifficultyFeedbackRepository;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordSummaryProjection;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.tracking.entity.TrackingPhoto;
import com.semosan.api.domain.tracking.repository.TrackingPhotoRepository;
import com.semosan.api.domain.tracking.repository.TrackingPointRepository;
import com.semosan.api.domain.tracking.repository.projection.TrackingPathProjection;
import com.semosan.api.common.exception.ConstraintViolationUtils;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HikingRecordService {

    private static final String DIFFICULTY_FEEDBACK_HIKING_RECORD_UNIQUE_CONSTRAINT =
            "uk_course_difficulty_feedback_hiking_record";

    private final HikingRecordRepository hikingRecordRepository;
    private final UserReader userReader;
    private final MountainRepository mountainRepository;
    private final HikingMemberRepository hikingMemberRepository;
    private final CourseDifficultyFeedbackRepository courseDifficultyFeedbackRepository;
    private final TrackingPointRepository trackingPointRepository;
    private final TrackingPhotoRepository trackingPhotoRepository;

    // 유저가 다녀온 산 목록을 산 단위로 묶어 조회합니다.
    @Transactional(readOnly = true)
    public Page<GetUserHikingMountainRecordResponse> getUserHikingMountainRecords(Long userId, Pageable pageable) {
        userReader.findCompletedOnboardingUserById(userId);
        return hikingRecordRepository.findUserHikingMountainRecordsByUserId(userId, pageable)
                .map(GetUserHikingMountainRecordResponse::from);
    }

    // 유저의 등산 기록 목록을 기록 단위로 조회합니다.
    @Transactional(readOnly = true)
    public Page<GetUserHikingRecordResponse> getUserHikingRecords(Long userId, Pageable pageable) {
        userReader.findCompletedOnboardingUserById(userId);
        return hikingRecordRepository.findUserHikingRecordsByUserId(userId, pageable)
                .map(GetUserHikingRecordResponse::from);
    }

    // 특정 산에 대한 유저의 등산 기록 목록을 기록 단위로 조회합니다.
    @Transactional(readOnly = true)
    public Page<GetUserHikingRecordResponse> getUserHikingRecordsByMountainId(
            Long userId,
            Long mountainId,
            Pageable pageable
    ) {
        userReader.findCompletedOnboardingUserById(userId);
        if (!mountainRepository.existsById(mountainId)) {
            throw new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND);
        }
        return hikingRecordRepository.findUserHikingRecordsByUserIdAndMountainId(userId, mountainId, pageable)
                .map(GetUserHikingRecordResponse::from);
    }

    // 등산 기록 단건 상세를 조회합니다. (이동 경로 + 마일스톤 사진 포함)
    @Transactional(readOnly = true)
    public HikingRecordDetailResponse getHikingRecordDetail(Long userId, Long hikingRecordId) {
        User user = userReader.findCompletedOnboardingUserById(userId);
        // Mountain / Course 를 fetch join 으로 함께 가져와 응답 조립 시 추가 SELECT 를 막는다.
        HikingRecord record = hikingRecordRepository.findWithMountainAndCourseById(hikingRecordId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.HIKING_RECORD_NOT_FOUND));

        if (!hikingMemberRepository.existsByHikingRecordAndUser(record, user)) {
            throw new GeneralException(ErrorStatus.HIKING_RECORD_FORBIDDEN);
        }

        String track = null;
        String altitudes = null;
        List<TrackingPhoto> photos = List.of();
        if (record.getTrackingSession() != null) {
            Long sessionId = record.getTrackingSession().getId();
            Optional<TrackingPathProjection> path = trackingPointRepository.findTrackBySessionId(sessionId);
            if (path.isPresent() && path.get().getTrack() != null) {
                track = path.get().getTrack();
                altitudes = path.get().getAltitudes();
            }
            photos = trackingPhotoRepository.findByTrackingSession_IdOrderByMilestoneIndexAsc(sessionId);
        }

        return HikingRecordDetailResponse.of(record, track, altitudes, photos);
    }

    // 유저의 등산 기록 요약 정보를 조회합니다.
    @Transactional(readOnly = true)
    public GetUserHikingRecordSummaryResponse getUserHikingRecordSummary(Long userId) {
        userReader.findCompletedOnboardingUserById(userId);
        UserHikingRecordSummaryProjection projection =
                hikingRecordRepository.findUserHikingRecordSummaryByUserId(userId);
        if (projection == null) {
            return GetUserHikingRecordSummaryResponse.empty();
        }
        return GetUserHikingRecordSummaryResponse.from(projection);
    }

    // 코스 기반 등산 기록에 대한 난이도 체감 피드백을 저장합니다.
    @Transactional
    public CourseDifficultyFeedbackResponse createCourseDifficultyFeedback(
            Long userId,
            Long hikingRecordId,
            CreateCourseDifficultyFeedbackRequest request
    ) {
        User user = userReader.findCompletedOnboardingUserById(userId);
        HikingRecord hikingRecord = hikingRecordRepository.findById(hikingRecordId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.HIKING_RECORD_NOT_FOUND));

        if (!hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, user)) {
            throw new GeneralException(ErrorStatus.HIKING_RECORD_FORBIDDEN);
        }
        if (hikingRecord.getCourse() == null) {
            throw new GeneralException(ErrorStatus.HIKING_RECORD_COURSE_REQUIRED);
        }
        if (courseDifficultyFeedbackRepository.existsByHikingRecord_Id(hikingRecordId)) {
            throw new GeneralException(ErrorStatus.COURSE_DIFFICULTY_FEEDBACK_ALREADY_EXISTS);
        }

        CourseDifficultyFeedback feedback = CourseDifficultyFeedback.create(
                hikingRecord,
                user,
                request.comparison()
        );
        try {
            return CourseDifficultyFeedbackResponse.from(courseDifficultyFeedbackRepository.saveAndFlush(feedback));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 유니크 제약 위반 시 이미 피드백이 존재하는 에러로 변환
            if (ConstraintViolationUtils.isViolation(e, DIFFICULTY_FEEDBACK_HIKING_RECORD_UNIQUE_CONSTRAINT)) {
                throw new GeneralException(ErrorStatus.COURSE_DIFFICULTY_FEEDBACK_ALREADY_EXISTS);
            }
            throw e;
        }
    }
}
