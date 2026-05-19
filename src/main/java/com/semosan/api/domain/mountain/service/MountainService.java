package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.*;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapResponse;
import com.semosan.api.domain.mountain.dto.response.MountainRecommendationResponse;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.AmenityType;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.*;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.review.service.ReviewService;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MountainService {

    // TODO: 실제 서비스 영역 확장 시 BBox 기본값 재조정 / 줌 레벨 기반 LOD 도입 검토
    private static final double DEFAULT_SEOUL_SW_LAT = 37.413;
    private static final double DEFAULT_SEOUL_SW_LNG = 126.764;
    private static final double DEFAULT_SEOUL_NE_LAT = 37.715;
    private static final double DEFAULT_SEOUL_NE_LNG = 127.184;

    private final MountainRepository mountainRepository;
    private final CourseRepository courseRepository;
    private final TransportationRepository transportationRepository;
    private final AmenityRepository amenityRepository;
    private final RestaurantSectionRepository restaurantSectionRepository;
    private final ReviewService reviewService;
    private final UserOnboardingRepository userOnboardingRepository;
    private final HikingMemberRepository hikingMemberRepository;

    public Page<MountainListResponse> getMountains(Pageable pageable) {
        return mountainRepository.findAll(pageable)
                .map(MountainListResponse::from);
    }

    public MountainMapListResponse getMountainsForMap(
            Long userId,
            Double swLat,
            Double swLng,
            Double neLat,
            Double neLng
    ) {
        // BBox 는 4개 좌표가 한 세트. 부분 입력은 의도 모호하므로 거부함. 4개 모두 비어있을 때만 default 적용.
        int provided = (swLat == null ? 0 : 1) + (swLng == null ? 0 : 1)
                + (neLat == null ? 0 : 1) + (neLng == null ? 0 : 1);
        if (provided != 0 && provided != 4) {
            throw new GeneralException(ErrorStatus.MOUNTAIN_BBOX_PARTIAL);
        }
        boolean useDefault = (provided == 0);
        double resolvedSwLat = useDefault ? DEFAULT_SEOUL_SW_LAT : swLat;
        double resolvedSwLng = useDefault ? DEFAULT_SEOUL_SW_LNG : swLng;
        double resolvedNeLat = useDefault ? DEFAULT_SEOUL_NE_LAT : neLat;
        double resolvedNeLng = useDefault ? DEFAULT_SEOUL_NE_LNG : neLng;

        List<MountainMapResponse> mountains = mountainRepository
                .findInBBoxWithUserHikingStats(userId, resolvedSwLat, resolvedSwLng, resolvedNeLat, resolvedNeLng)
                .stream()
                .map(MountainMapResponse::from)
                .toList();
        boolean hasHikingRecord = hikingMemberRepository.existsByUser_Id(userId);
        return MountainMapListResponse.of(hasHikingRecord, mountains);
    }

    /**
     * 로그인 사용자의 등산 레벨에 맞는 산을 "사용자 위치에서 가까운 순"으로 추천한다.
     *  - 온보딩이 완료된 사용자: HikingLevel → Difficulty 집합 매핑 (임의로 해둠) TODO: 필터링 로직 확정되어야함
     *  - 온보딩 미완료(UserOnboarding 없음): 모든 난이도 fallback
     *  - 정렬: squared distance ASC (사용자 lat/lng 기준 가까운 순). 자세한 정렬 원리는 Repository 주석 참고.
     *  - 다녀온 산 제외 여부: 현재는 포함. 기획에 따라 추후 조정
     */
    public Page<MountainRecommendationResponse> getRecommendedMountains(
            Long userId,
            Double lat,
            Double lng,
            Pageable pageable
    ) {
        Set<Difficulty> difficulties = userOnboardingRepository.findByUser_Id(userId)
                .map(UserOnboarding::getHikingLevel)
                .map(MountainService::mapHikingLevelToDifficulties)
                .orElseGet(() -> EnumSet.allOf(Difficulty.class));

        List<String> difficultyNames = difficulties.stream()
                .map(Enum::name)
                .toList();

        return mountainRepository.findRecommendationsByDifficulties(difficultyNames, lat, lng, pageable)
                .map(MountainRecommendationResponse::from);
    }

    /**
     * HikingLevel → Difficulty 매핑 (임의 정의).
     * TODO: 추천 정책이 확정되면 정식 로직으로 교체 예정이오
     */
    private static Set<Difficulty> mapHikingLevelToDifficulties(HikingLevel level) {
        return switch (level) {
            case BEGINNER -> EnumSet.of(Difficulty.EASY);
            case HOBBY -> EnumSet.of(Difficulty.EASY, Difficulty.NORMAL);
            case EXPERIENCED -> EnumSet.of(Difficulty.NORMAL);
            case EXPERT -> EnumSet.of(Difficulty.NORMAL, Difficulty.HARD);
        };
    }

    public Page<MountainListResponse> searchMountains(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        return mountainRepository.searchByKeyword(keyword.trim(), pageable)
                .map(MountainListResponse::from);
    }

    public MountainDetailResponse getMountainDetail(Long mountainId) {
        Mountain mountain = findMountainById(mountainId);

        return new MountainDetailResponse(
                MountainInfo.from(mountain),
                findCourses(mountainId),
                findTransportationGroup(mountainId),
                findAmenitiesByDirection(mountainId),
                findRestaurantSections(mountainId),
                findReviews(mountainId)
        );
    }

    private Mountain findMountainById(Long mountainId) {
        return mountainRepository.findById(mountainId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND));
    }

    private List<CourseInfo> findCourses(Long mountainId) {
        return courseRepository.findByMountainId(mountainId).stream()
                .map(CourseInfo::from)
                .toList();
    }

    private TransportationGroup findTransportationGroup(Long mountainId) {
        return TransportationGroup.from(transportationRepository.findByMountainId(mountainId));
    }

    private Map<String, List<AmenityType>> findAmenitiesByDirection(Long mountainId) {
        return MountainDetailResponse.groupAmenities(amenityRepository.findByMountainId(mountainId));
    }

    private List<RestaurantSectionInfo> findRestaurantSections(Long mountainId) {
        return restaurantSectionRepository.findByMountainIdWithRestaurants(mountainId).stream()
                .map(RestaurantSectionInfo::from)
                .toList();
    }

    private List<ReviewInfo> findReviews(Long mountainId) {
        return reviewService.getReviewsByMountainId(mountainId).stream()
                .map(ReviewInfo::from)
                .toList();
    }
}
