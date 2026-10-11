package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.dto.response.MountainLikeToggleResponse;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        validateMountainExists(mountainId);

        if (mountainLikeRepository.deleteByUser_IdAndMountain_Id(userId, mountainId) > 0) {
            return new MountainLikeToggleResponse(false);
        }
        // ON CONFLICT DO NOTHING이라 동시 요청이 겹쳐도 예외 없이 0 row로 끝난다.
        mountainLikeRepository.insertIgnoreConflict(userId, mountainId);
        return new MountainLikeToggleResponse(true);
    }

    // 로그인한 사용자가 좋아요한 산 목록을 조회합니다.
    @Transactional(readOnly = true)
    public Page<LikedMountainResponse> getLikedMountains(Long userId, Pageable pageable) {
        // 탈퇴 후 남은 access token으로 조회되는 것을 방지합니다.
        userReader.findActiveUserById(userId);
        // 정렬은 쿼리에 고정되어 있다. Pageable.sort 를 그대로 넘기면 JPQL 의 ORDER BY 뒤에 덧붙는다.
        // unpaged 는 getPageNumber() 자체가 UnsupportedOperationException 이라 그대로 넘긴다.
        Pageable unsorted = pageable.isUnpaged()
                ? Pageable.unpaged()
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return mountainLikeRepository.findAllByUserId(userId, unsorted)
                .map(LikedMountainResponse::from);
    }

    // 존재 확인만 필요하므로 엔티티를 읽지 않는다. Mountain 은 PostGIS 좌표와 jsonb 를 들고 있어 로드 비용이 크다.
    private void validateMountainExists(Long mountainId) {
        if (!mountainRepository.existsById(mountainId)) {
            throw new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND);
        }
    }
}
