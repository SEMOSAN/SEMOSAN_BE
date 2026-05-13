package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.*;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.AmenityType;
import com.semosan.api.domain.mountain.repository.*;
import com.semosan.api.domain.review.service.ReviewService;
import com.semosan.api.domain.user.service.UserReader;
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

    private final MountainRepository mountainRepository;
    private final CourseRepository courseRepository;
    private final TransportationRepository transportationRepository;
    private final AmenityRepository amenityRepository;
    private final RestaurantSectionRepository restaurantSectionRepository;
    private final ReviewService reviewService;
    private final UserReader userReader;

    public Page<MountainListResponse> getMountains(Long userId, Pageable pageable) {
        userReader.findCompletedOnboardingUserById(userId);
        return mountainRepository.findAll(pageable)
                .map(MountainListResponse::from);
    }

    public Page<MountainListResponse> searchMountains(Long userId, String keyword, Pageable pageable) {
        userReader.findCompletedOnboardingUserById(userId);
        if (keyword == null || keyword.isBlank()) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        return mountainRepository.searchByKeyword(keyword.trim(), pageable)
                .map(MountainListResponse::from);
    }

    public MountainDetailResponse getMountainDetail(Long userId, Long mountainId) {
        userReader.findCompletedOnboardingUserById(userId);
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
