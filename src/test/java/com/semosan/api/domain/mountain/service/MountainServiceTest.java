package com.semosan.api.domain.mountain.service;

import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.mountain.dto.response.MountainRecommendationResponse;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.AmenityRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.mountain.repository.RestaurantSectionRepository;
import com.semosan.api.domain.mountain.repository.TransportationRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

        when(userReader.findCompletedOnboardingByUserId(1L)).thenReturn(onboarding);
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
        ReflectionTestUtils.setField(mountain, "imageUrls", List.of());
        return mountain;
    }

    private Course course(Mountain mountain) throws Exception {
        Constructor<Course> constructor = Course.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Course course = constructor.newInstance();
        ReflectionTestUtils.setField(course, "mountain", mountain);
        return course;
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
