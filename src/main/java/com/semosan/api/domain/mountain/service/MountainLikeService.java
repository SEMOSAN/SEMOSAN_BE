package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.dto.response.MountainLikeToggleResponse;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public MountainLikeToggleResponse toggleMountainLike(Long userId, Long mountainId) {
        userReader.findActiveUserById(userId);
        findMountainById(mountainId);

        boolean liked = mountainLikeRepository.findByUser_IdAndMountain_Id(userId, mountainId)
                .map(existing -> {
                    mountainLikeRepository.delete(existing);
                    return false;
                })
                // ON CONFLICT DO NOTHING이라 동시 요청이 겹쳐도 예외 없이 0 row로 끝난다.
                .orElseGet(() -> {
                    mountainLikeRepository.insertIgnoreConflict(userId, mountainId);
                    return true;
                });
        return new MountainLikeToggleResponse(liked);
    }

    // 로그인한 사용자가 좋아요한 산 목록을 조회합니다.
    @Transactional(readOnly = true)
    public Page<LikedMountainResponse> getLikedMountains(Long userId, Pageable pageable) {
        // 탈퇴 후 남은 access token으로 조회되는 것을 방지합니다.
        userReader.findActiveUserById(userId);
        return mountainLikeRepository.findAllByUserId(userId, pageable)
                .map(LikedMountainResponse::from);
    }

    // mountainId로 산을 조회하고, 없으면 예외를 발생시킵니다.
    private Mountain findMountainById(Long mountainId) {
        return mountainRepository.findById(mountainId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND));
    }
}
