package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainRecommendationResponse;
import com.semosan.api.domain.mountain.entity.Amenity;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.entity.Restaurant;
import com.semosan.api.domain.mountain.entity.RestaurantSection;
import com.semosan.api.domain.mountain.entity.Transportation;
import com.semosan.api.domain.mountain.enums.AmenityType;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.enums.TransportationType;
import com.semosan.api.domain.mountain.repository.AmenityRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.mountain.repository.RestaurantSectionRepository;
import com.semosan.api.domain.mountain.repository.TransportationRepository;
import com.semosan.api.domain.mountain.repository.projection.MountainMapProjection;
import com.semosan.api.domain.review.entity.Review;
import com.semosan.api.domain.mountain.service.recommendation.FitnessLevelCalculator;
import com.semosan.api.domain.mountain.service.recommendation.TrackScorer;
import com.semosan.api.domain.review.service.ReviewService;
import com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.Gender;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MountainServiceTest {

    @Mock
    private MountainRepository mountainRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TransportationRepository transportationRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private RestaurantSectionRepository restaurantSectionRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private HikingMemberRepository hikingMemberRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private FitnessLevelCalculator fitnessLevelCalculator;

    @Mock
    private TrackScorer trackScorer;

    @InjectMocks
    private MountainService mountainService;

    @Test
    void getMountainsReturnsPublicMountains() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainRepository.findByIsPublicTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(mountain(1L, "관악산")), pageable, 1));

        Page<MountainListResponse> result = mountainService.getMountains(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().mountainId()).isEqualTo(1L);
        assertThat(result.getContent().getFirst().name()).isEqualTo("관악산");
    }

    @Test
    void searchMountainsTrimsKeywordAndMapsResult() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainRepository.searchByKeyword("관악", pageable))
                .thenReturn(new PageImpl<>(List.of(mountain(1L, "관악산")), pageable, 1));

        Page<MountainListResponse> result = mountainService.searchMountains(1L, "  관악  ", pageable);

        assertThat(result.getContent().getFirst().name()).isEqualTo("관악산");
    }

    @Test
    void searchMountainsThrowsWhenKeywordBlank() {
        assertThatThrownBy(() -> mountainService.searchMountains(1L, "   ", PageRequest.of(0, 10)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.BAD_REQUEST);
    }

    @Test
    void getMountainsForMapUsesDefaultBBoxWhenCoordinatesAreMissing() {
        MountainMapProjection projection = mapProjection(1L, 2L);
        when(mountainRepository.findInBBoxWithUserHikingStats(
                1L, 37.413, 126.764, 37.715, 127.184))
                .thenReturn(List.of(projection));
        when(hikingMemberRepository.existsByUser_Id(1L)).thenReturn(true);

        MountainMapListResponse response = mountainService.getMountainsForMap(1L, null, null, null, null);

        assertThat(response.hasHikingRecord()).isTrue();
        assertThat(response.mountains()).hasSize(1);
        assertThat(response.mountains().getFirst().visited()).isTrue();
        assertThat(response.mountains().getFirst().visitCount()).isEqualTo(2L);
    }

    @Test
    void getMountainsForMapUsesProvidedBBox() {
        MountainMapProjection projection = mapProjection(1L, 0L);
        when(mountainRepository.findInBBoxWithUserHikingStats(
                1L, 37.0, 126.0, 38.0, 127.0))
                .thenReturn(List.of(projection));
        when(hikingMemberRepository.existsByUser_Id(1L)).thenReturn(false);

        MountainMapListResponse response = mountainService.getMountainsForMap(1L, 37.0, 126.0, 38.0, 127.0);

        assertThat(response.hasHikingRecord()).isFalse();
        assertThat(response.mountains().getFirst().visited()).isFalse();
    }

    @Test
    void getMountainsForMapThrowsWhenBBoxIsPartial() {
        assertThatThrownBy(() -> mountainService.getMountainsForMap(1L, 37.0, null, 38.0, 127.0))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_BBOX_PARTIAL);
    }

    @Test
    void getMountainDetailReturnsGroupedDetailData() throws Exception {
        Mountain mountain = mountain(1L, "관악산");
        Course course = course(mountain);
        ReflectionTestUtils.setField(course, "id", 10L);
        ReflectionTestUtils.setField(course, "name", "정상 코스");
        ReflectionTestUtils.setField(course, "difficulty", Difficulty.NORMAL);
        ReflectionTestUtils.setField(course, "distance", 1500.0);
        ReflectionTestUtils.setField(course, "duration", 90);
        ReflectionTestUtils.setField(course, "startName", "입구");
        ReflectionTestUtils.setField(course, "endName", "정상");
        Transportation bus = transportation(1L, TransportationType.BUS, "상행", "버스", "버스 설명");
        Transportation parking = transportation(2L, TransportationType.PARKING, "입구", "주차장", "주차 설명");
        Amenity restroom = amenity(AmenityType.RESTROOM, "입구");
        RestaurantSection section = RestaurantSection.create(mountain, "근처 맛집");
        Restaurant restaurant = Restaurant.create(section, "식당", "한식", "메뉴", "설명", "image", "map", "blog");
        ReflectionTestUtils.setField(restaurant, "id", 30L);
        ReflectionTestUtils.setField(section, "restaurants", List.of(restaurant));
        Review review = review(40L, user(), course);

        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.findByMountainId(1L)).thenReturn(List.of(course));
        when(transportationRepository.findByMountainId(1L)).thenReturn(List.of(bus, parking));
        when(amenityRepository.findByMountainId(1L)).thenReturn(List.of(restroom));
        when(restaurantSectionRepository.findByMountainIdWithRestaurants(1L)).thenReturn(List.of(section));
        when(reviewService.getReviewsByMountainId(1L)).thenReturn(List.of(review));

        MountainDetailResponse response = mountainService.getMountainDetail(1L, 1L);

        assertThat(response.mountain().mountainId()).isEqualTo(1L);
        assertThat(response.courses().getFirst().courseId()).isEqualTo(10L);
        assertThat(response.transportations().publicTransport()).containsKey("상행");
        assertThat(response.transportations().parking()).containsKey("입구");
        assertThat(response.amenities()).containsEntry("입구", List.of(AmenityType.RESTROOM));
        assertThat(response.restaurantSections().getFirst().restaurants().getFirst().restaurantId()).isEqualTo(30L);
        assertThat(response.reviews().getFirst().reviewId()).isEqualTo(40L);
        assertThat(response.reviews().getFirst().courseName()).isEqualTo("정상 코스");
    }

    @Test
    void getMountainDetailThrowsWhenMountainMissing() {
        when(mountainRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mountainService.getMountainDetail(1L, 1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void getRecommendedMountainsReturnsTopThreeMountainsByTrackScore() throws Exception {
        User user = user();
        UserOnboarding onboarding = onboarding(user);
        Mountain first = mountain(1L, "첫번째산");
        Mountain second = mountain(2L, "두번째산");
        Mountain third = mountain(3L, "세번째산");
        Mountain fourth = mountain(4L, "네번째산");
        Course firstCourse = course(first);
        Course secondCourse = course(second);
        Course thirdCourse = course(third);
        Course fourthCourse = course(fourth);

        when(userReader.findOnboardingByUserId(1L)).thenReturn(Optional.of(onboarding));
        when(courseRepository.findAllWithMountainForRecommendation())
                .thenReturn(List.of(fourthCourse, secondCourse, firstCourse, thirdCourse));
        when(fitnessLevelCalculator.calculate(user, onboarding))
                .thenReturn(com.semosan.api.domain.mountain.enums.FitnessLevel.INTERMEDIATE);
        when(trackScorer.evaluate(firstCourse, com.semosan.api.domain.mountain.enums.FitnessLevel.INTERMEDIATE))
                .thenReturn(evaluation(firstCourse, true, 100));
        when(trackScorer.evaluate(secondCourse, com.semosan.api.domain.mountain.enums.FitnessLevel.INTERMEDIATE))
                .thenReturn(evaluation(secondCourse, true, 80));
        when(trackScorer.evaluate(thirdCourse, com.semosan.api.domain.mountain.enums.FitnessLevel.INTERMEDIATE))
                .thenReturn(evaluation(thirdCourse, true, 60));
        when(trackScorer.evaluate(fourthCourse, com.semosan.api.domain.mountain.enums.FitnessLevel.INTERMEDIATE))
                .thenReturn(evaluation(fourthCourse, true, 40));

        List<MountainRecommendationResponse> result = mountainService.getRecommendedMountains(
                1L,
                37.0,
                127.0
        );

        assertThat(result)
                .extracting(MountainRecommendationResponse::name)
                .containsExactly("첫번째산", "두번째산", "세번째산");
        assertThat(result).hasSize(3);
        assertThat(result.get(0).difficultyLabel()).isEqualTo("중");
        assertThat(result.get(0).mountainHeightM()).isEqualTo(123);
    }

    @Test
    void getRecommendedMountainsSelectsBetterCandidatePerMountain() throws Exception {
        User user = user();
        UserOnboarding onboarding = onboarding(user);
        Mountain mountain = mountain(1L, "관악산");
        Course eligibleCourse = course(mountain);
        Course ineligibleCourse = course(mountain);

        when(userReader.findOnboardingByUserId(1L)).thenReturn(Optional.of(onboarding));
        when(courseRepository.findAllWithMountainForRecommendation())
                .thenReturn(List.of(ineligibleCourse, eligibleCourse));
        when(fitnessLevelCalculator.calculate(user, onboarding))
                .thenReturn(com.semosan.api.domain.mountain.enums.FitnessLevel.INTERMEDIATE);
        when(trackScorer.evaluate(ineligibleCourse, com.semosan.api.domain.mountain.enums.FitnessLevel.INTERMEDIATE))
                .thenReturn(evaluation(ineligibleCourse, false, 200));
        when(trackScorer.evaluate(eligibleCourse, com.semosan.api.domain.mountain.enums.FitnessLevel.INTERMEDIATE))
                .thenReturn(evaluation(eligibleCourse, true, 10));

        List<MountainRecommendationResponse> result = mountainService.getRecommendedMountains(1L, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("관악산");
    }

    @Test
    void getRecommendedMountainsReturnsDefaultMountainsWhenOnboardingIsIncomplete() throws Exception {
        User user = User.createTestUser("recommendation-incomplete-user", DeviceType.IOS);
        Mountain first = mountain(1L, "북한산");
        Mountain second = mountain(5L, "아차산");
        Mountain third = mountain(8L, "관악산");

        when(userReader.findOnboardingByUserId(1L)).thenReturn(Optional.of(onboarding(user)));
        when(mountainRepository.findAllById(List.of(1L, 5L, 8L)))
                .thenReturn(List.of(third, first, second));

        List<MountainRecommendationResponse> result = mountainService.getRecommendedMountains(
                1L,
                37.0,
                127.0
        );

        assertThat(result)
                .extracting(MountainRecommendationResponse::mountainId)
                .containsExactly(1L, 5L, 8L);
        assertThat(result)
                .extracting(MountainRecommendationResponse::name)
                .containsExactly("북한산", "아차산", "관악산");
        verifyNoInteractions(courseRepository, fitnessLevelCalculator, trackScorer);
    }

    @Test
    void getRecommendedMountainsReturnsDefaultMountainsWhenOnboardingRowIsMissing() throws Exception {
        User user = User.createTestUser("recommendation-no-onboarding-user", DeviceType.IOS);
        Mountain first = mountain(1L, "북한산");
        Mountain second = mountain(5L, "아차산");
        Mountain third = mountain(8L, "관악산");

        when(userReader.findOnboardingByUserId(1L)).thenReturn(Optional.empty());
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(mountainRepository.findAllById(List.of(1L, 5L, 8L)))
                .thenReturn(List.of(third, first, second));

        List<MountainRecommendationResponse> result = mountainService.getRecommendedMountains(
                1L,
                37.0,
                127.0
        );

        assertThat(result)
                .extracting(MountainRecommendationResponse::mountainId)
                .containsExactly(1L, 5L, 8L);
        assertThat(result)
                .extracting(MountainRecommendationResponse::name)
                .containsExactly("북한산", "아차산", "관악산");
        verifyNoInteractions(courseRepository, fitnessLevelCalculator, trackScorer);
    }

    private User user() {
        User user = User.createTestUser("recommendation-test-user", DeviceType.IOS);
        user.completeOnboarding(new CompleteOnboardingCommand(
                "테스트",
                null,
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                175.0,
                70.0
        ));
        return user;
    }

    private UserOnboarding onboarding(User user) {
        return UserOnboarding.create(new CreateUserOnboardingCommand(
                user,
                HikingLevel.EXPERT,
                ExerciseType.HIKING,
                ExerciseFrequency.DAILY,
                ExerciseDuration.OVER_4H
        ));
    }

    private Mountain mountain(Long id, String name) throws Exception {
        Constructor<Mountain> constructor = Mountain.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Mountain mountain = constructor.newInstance();
        ReflectionTestUtils.setField(mountain, "id", id);
        ReflectionTestUtils.setField(mountain, "name", name);
        ReflectionTestUtils.setField(mountain, "difficulty", Difficulty.NORMAL);
        ReflectionTestUtils.setField(mountain, "altitude", 500.0);
        ReflectionTestUtils.setField(mountain, "address", "주소");
        ReflectionTestUtils.setField(mountain, "duration", 120);
        ReflectionTestUtils.setField(mountain, "imageUrls", List.of("image.jpg"));
        ReflectionTestUtils.setField(mountain, "latitude", 37.5);
        ReflectionTestUtils.setField(mountain, "longitude", 127.0);
        return mountain;
    }

    private Course course(Mountain mountain) throws Exception {
        Constructor<Course> constructor = Course.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Course course = constructor.newInstance();
        ReflectionTestUtils.setField(course, "mountain", mountain);
        return course;
    }

    private MountainMapProjection mapProjection(Long id, Long visitCount) {
        MountainMapProjection projection = mock(MountainMapProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getName()).thenReturn("관악산");
        when(projection.getLatitude()).thenReturn(37.5);
        when(projection.getLongitude()).thenReturn(127.0);
        when(projection.getVisitCount()).thenReturn(visitCount);
        when(projection.getImageUrl()).thenReturn("image.jpg");
        return projection;
    }

    private Transportation transportation(
            Long id,
            TransportationType type,
            String direction,
            String name,
            String description
    ) {
        Transportation transportation = Transportation.create(mountainForRelation(), type, direction, name, description);
        ReflectionTestUtils.setField(transportation, "id", id);
        return transportation;
    }

    private Amenity amenity(AmenityType type, String direction) {
        Amenity amenity = newInstance(Amenity.class);
        ReflectionTestUtils.setField(amenity, "type", type);
        ReflectionTestUtils.setField(amenity, "direction", direction);
        return amenity;
    }

    private Review review(Long id, User user, Course course) {
        Review review = newInstance(Review.class);
        ReflectionTestUtils.setField(review, "id", id);
        ReflectionTestUtils.setField(review, "user", user);
        ReflectionTestUtils.setField(review, "course", course);
        ReflectionTestUtils.setField(review, "content", "리뷰");
        ReflectionTestUtils.setField(review, "difficulty", Difficulty.EASY);
        ReflectionTestUtils.setField(review, "imageUrl", "review.jpg");
        return review;
    }

    private Mountain mountainForRelation() {
        Mountain mountain = newInstance(Mountain.class);
        ReflectionTestUtils.setField(mountain, "id", 1L);
        ReflectionTestUtils.setField(mountain, "name", "관악산");
        return mountain;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private TrackScorer.TrackEvaluation evaluation(Course course, boolean eligible, double score) {
        return new TrackScorer.TrackEvaluation(
                course,
                new TrackScorer.TrackMetrics(1, 1, 123, 0, 0, 0, 0),
                eligible,
                score,
                score
        );
    }
}
