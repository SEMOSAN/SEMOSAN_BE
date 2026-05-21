package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.entity.MountainLike;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MountainLikeService {

    private final MountainLikeRepository mountainLikeRepository;
    private final MountainRepository mountainRepository;
    private final UserReader userReader;

    // 로그인한 사용자가 산에 좋아요를 등록합니다.
    @Transactional
    public void likeMountain(Long userId, Long mountainId) {
        User user = userReader.findCompletedOnboardingUserById(userId);
        Mountain mountain = findMountainById(mountainId);
        if (mountainLikeRepository.existsByUser_IdAndMountain_Id(userId, mountainId)) {
            throw new GeneralException(ErrorStatus.MOUNTAIN_LIKE_ALREADY_EXISTS);
        }

        try {
            mountainLikeRepository.save(MountainLike.create(user, mountain));
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ErrorStatus.MOUNTAIN_LIKE_ALREADY_EXISTS);
        }
    }

    // 로그인한 사용자가 산 좋아요를 취소합니다.
    @Transactional
    public void unlikeMountain(Long userId, Long mountainId) {
        // 탈퇴 후 남은 access token으로 조회되는 것을 방지합니다.
        userReader.findCompletedOnboardingUserById(userId);
        MountainLike mountainLike = mountainLikeRepository.findByUser_IdAndMountain_Id(userId, mountainId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_LIKE_NOT_FOUND));
        mountainLikeRepository.delete(mountainLike);
    }

    // 로그인한 사용자가 좋아요한 산 목록을 조회합니다.
    @Transactional(readOnly = true)
    public Page<LikedMountainResponse> getLikedMountains(Long userId, Pageable pageable) {
        // 탈퇴 후 남은 access token으로 조회되는 것을 방지합니다.
        userReader.findCompletedOnboardingUserById(userId);
        return mountainLikeRepository.findAllByUserId(userId, pageable)
                .map(LikedMountainResponse::from);
    }

    // mountainId로 산을 조회하고, 없으면 예외를 발생시킵니다.
    private Mountain findMountainById(Long mountainId) {
        return mountainRepository.findById(mountainId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND));
    }
}
