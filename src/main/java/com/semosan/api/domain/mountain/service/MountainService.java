package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.*;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapResponse;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.AmenityType;
import com.semosan.api.domain.mountain.repository.*;
import com.semosan.api.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
        boolean hasFullBBox = swLat != null && swLng != null && neLat != null && neLng != null;
        double resolvedSwLat = hasFullBBox ? swLat : DEFAULT_SEOUL_SW_LAT;
        double resolvedSwLng = hasFullBBox ? swLng : DEFAULT_SEOUL_SW_LNG;
        double resolvedNeLat = hasFullBBox ? neLat : DEFAULT_SEOUL_NE_LAT;
        double resolvedNeLng = hasFullBBox ? neLng : DEFAULT_SEOUL_NE_LNG;

        List<MountainMapResponse> mountains = mountainRepository
                .findInBBoxWithUserHikingStats(userId, resolvedSwLat, resolvedSwLng, resolvedNeLat, resolvedNeLng)
                .stream()
                .map(MountainMapResponse::from)
                .toList();
        return MountainMapListResponse.from(mountains);
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
