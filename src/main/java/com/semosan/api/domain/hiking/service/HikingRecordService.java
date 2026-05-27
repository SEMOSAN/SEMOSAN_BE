package com.semosan.api.domain.hiking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.hiking.dto.request.CreateCourseDifficultyFeedbackRequest;
import com.semosan.api.domain.hiking.dto.response.CourseDifficultyFeedbackResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingMountainRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordSummaryResponse;
import com.semosan.api.domain.hiking.entity.CourseDifficultyFeedback;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.repository.CourseDifficultyFeedbackRepository;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordSummaryProjection;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HikingRecordService {

    private final HikingRecordRepository hikingRecordRepository;
    private final UserReader userReader;
    private final MountainRepository mountainRepository;
    private final HikingMemberRepository hikingMemberRepository;
    private final CourseDifficultyFeedbackRepository courseDifficultyFeedbackRepository;

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
        return CourseDifficultyFeedbackResponse.from(courseDifficultyFeedbackRepository.save(feedback));
    }
}
