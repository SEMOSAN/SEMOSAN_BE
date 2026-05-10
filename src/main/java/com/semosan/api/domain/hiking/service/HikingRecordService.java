package com.semosan.api.domain.hiking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingMountainRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordSummaryResponse;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordSummaryProjection;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HikingRecordService {

    private final HikingRecordRepository hikingRecordRepository;
    private final UserRepository userRepository;

    // 유저가 다녀온 산 목록을 산 단위로 묶어 조회합니다.
    @Transactional(readOnly = true)
    public Page<GetUserHikingMountainRecordResponse> getUserHikingMountainRecords(Long userId, Pageable pageable) {
        findActiveUserById(userId);
        return hikingRecordRepository.findUserHikingMountainRecordsByUserId(userId, pageable)
                .map(GetUserHikingMountainRecordResponse::from);
    }

    // 유저의 등산 기록 목록을 기록 단위로 조회합니다.
    @Transactional(readOnly = true)
    public Page<GetUserHikingRecordResponse> getUserHikingRecords(Long userId, Pageable pageable) {
        findActiveUserById(userId);
        return hikingRecordRepository.findUserHikingRecordsByUserId(userId, pageable)
                .map(GetUserHikingRecordResponse::from);
    }

    // 유저의 등산 기록 요약 정보를 조회합니다.
    @Transactional(readOnly = true)
    public GetUserHikingRecordSummaryResponse getUserHikingRecordSummary(Long userId) {
        findActiveUserById(userId);
        UserHikingRecordSummaryProjection projection =
                hikingRecordRepository.findUserHikingRecordSummaryByUserId(userId);
        if (projection == null) {
            return GetUserHikingRecordSummaryResponse.empty();
        }
        return GetUserHikingRecordSummaryResponse.from(projection);
    }

    // 활성 상태의 유저를 조회합니다.
    private User findActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }
}
