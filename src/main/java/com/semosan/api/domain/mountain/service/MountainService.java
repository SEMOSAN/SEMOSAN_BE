package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.*;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapResponse;
import com.semosan.api.domain.mountain.dto.response.MountainRecommendationResponse;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.AmenityType;
import com.semosan.api.domain.mountain.enums.FitnessLevel;
import com.semosan.api.domain.mountain.repository.*;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.review.service.ReviewService;
import com.semosan.api.domain.mountain.service.recommendation.FitnessLevelCalculator;
import com.semosan.api.domain.mountain.service.recommendation.TrackScorer;
import com.semosan.api.domain.mountain.service.recommendation.TrackScorer.TrackEvaluation;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.repository.UserOnboardingRepository;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private final UserOnboardingRepository userOnboardingRepository;
    private final HikingMemberRepository hikingMemberRepository;
    private final UserReader userReader;
    private final FitnessLevelCalculator fitnessLevelCalculator;
    private final TrackScorer trackScorer;

    public Page<MountainListResponse> getMountains(Long userId, Pageable pageable) {
        userReader.findCompletedOnboardingUserById(userId);
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

    public List<MountainRecommendationResponse> getRecommendedMountains(
            Long userId,
            Double lat,
            Double lng
    ) {
        User user = userReader.findCompletedOnboardingUserById(userId);
        UserOnboarding onboarding = userOnboardingRepository.findByUser_Id(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ONBOARDING_NOT_FOUND));
        FitnessLevel fitnessLevel = fitnessLevelCalculator.calculate(user, onboarding);

        Map<Long, RecommendationCandidate> candidateByMountainId = new LinkedHashMap<>();
        for (Course course : courseRepository.findAllWithMountainForRecommendation()) {
            TrackEvaluation evaluation = trackScorer.evaluate(course, fitnessLevel);
            candidateByMountainId.merge(
                    course.getMountain().getId(),
                    new RecommendationCandidate(course.getMountain(), evaluation),
                    MountainService::selectBetterCandidate
            );
        }

        return candidateByMountainId.values().stream()
                .sorted(RecommendationCandidate.recommendationOrder())
                .limit(3)
                .map(candidate -> MountainRecommendationResponse.from(
                        candidate.mountain(),
                        candidate.evaluation().metrics()
                ))
                .toList();
    }

    private static RecommendationCandidate selectBetterCandidate(
            RecommendationCandidate current,
            RecommendationCandidate next
    ) {
        if (current.evaluation().eligible() != next.evaluation().eligible()) {
            return next.evaluation().eligible() ? next : current;
        }
        return next.rankingScore() > current.rankingScore() ? next : current;
    }

    private record RecommendationCandidate(Mountain mountain, TrackEvaluation evaluation) {

        private double rankingScore() {
            return evaluation.eligible() ? evaluation.score() : evaluation.fallbackScore();
        }

        private static Comparator<RecommendationCandidate> recommendationOrder() {
            return Comparator
                    .comparing((RecommendationCandidate candidate) -> candidate.evaluation().eligible()).reversed()
                    .thenComparing(RecommendationCandidate::rankingScore, Comparator.reverseOrder())
                    .thenComparing(candidate -> candidate.mountain().getId());
        }
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
