package com.semosan.api.domain.mountain.service.recommendation;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.enums.FitnessLevel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackScorerTest {

    private final TrackScorer scorer = new TrackScorer();

    @Test
    void evaluatesEntryEligibleTrackWithDerivedMetrics() throws Exception {
        Course course = course(
                5000.0,
                List.of(100.0, 450.0),
                List.of("REST", "TRANS", "PARK", "VIEW", "PEAK")
        );

        TrackScorer.TrackEvaluation evaluation = scorer.evaluate(course, FitnessLevel.ENTRY);

        assertThat(evaluation.eligible()).isTrue();
        assertThat(evaluation.metrics().distanceKm()).isEqualTo(5.0);
        assertThat(evaluation.metrics().gain()).isEqualTo(350.0);
        assertThat(evaluation.metrics().mountainHeightM()).isEqualTo(450);
        assertThat(evaluation.metrics().restCount()).isEqualTo(1);
        assertThat(evaluation.metrics().accessCount()).isEqualTo(2);
        assertThat(evaluation.metrics().scenicCount()).isEqualTo(2);
        assertThat(evaluation.score()).isEqualTo(-12.5);
    }

    @Test
    void marksEntryTrackWithDangerAsIneligibleAndAppliesFallbackPenalty() throws Exception {
        Course course = course(
                5000.0,
                List.of(100.0, 300.0),
                List.of("DANGER", "REST")
        );

        TrackScorer.TrackEvaluation evaluation = scorer.evaluate(course, FitnessLevel.ENTRY);

        assertThat(evaluation.eligible()).isFalse();
        assertThat(evaluation.metrics().dangerCount()).isEqualTo(1);
        assertThat(evaluation.fallbackScore()).isEqualTo(evaluation.score() - 20);
    }

    @Test
    void scoresIntermediateTrackAroundTargetDistanceAndGainHigher() throws Exception {
        Course nearTarget = course(
                10000.0,
                List.of(100.0, 800.0),
                List.of("VIEW", "PHOTO", "PEAK")
        );
        Course farFromTarget = course(
                4000.0,
                List.of(100.0, 300.0),
                List.of("VIEW", "PHOTO", "PEAK")
        );

        TrackScorer.TrackEvaluation nearEvaluation = scorer.evaluate(nearTarget, FitnessLevel.INTERMEDIATE);
        TrackScorer.TrackEvaluation farEvaluation = scorer.evaluate(farFromTarget, FitnessLevel.INTERMEDIATE);

        assertThat(nearEvaluation.eligible()).isTrue();
        assertThat(farEvaluation.eligible()).isTrue();
        assertThat(nearEvaluation.score()).isGreaterThan(farEvaluation.score());
    }

    @Test
    void advancedLevelAllowsLongAndHighGainTrack() throws Exception {
        Course course = course(
                18000.0,
                List.of(100.0, 1300.0),
                List.of("DANGER", "VIEW", "PEAK")
        );

        TrackScorer.TrackEvaluation evaluation = scorer.evaluate(course, FitnessLevel.ADVANCED);

        assertThat(evaluation.eligible()).isTrue();
        assertThat(evaluation.metrics().gain()).isEqualTo(1200.0);
    }

    @Test
    void beginnerLevelRejectsTrackWithTooManyDangerWaypoints() throws Exception {
        Course course = course(
                8000.0,
                List.of(100.0, 500.0),
                List.of("DANGER", "DANGER", "REST")
        );

        TrackScorer.TrackEvaluation evaluation = scorer.evaluate(course, FitnessLevel.BEGINNER);

        assertThat(evaluation.eligible()).isFalse();
        assertThat(evaluation.metrics().dangerCount()).isEqualTo(2);
    }

    @Test
    void beginnerLevelRejectsTrackWhenDistanceOrGainExceedsLimit() throws Exception {
        TrackScorer.TrackEvaluation longTrack = scorer.evaluate(
                course(9500.0, List.of(100.0, 300.0), List.of()),
                FitnessLevel.BEGINNER
        );
        TrackScorer.TrackEvaluation highGainTrack = scorer.evaluate(
                course(8000.0, List.of(100.0, 750.0), List.of()),
                FitnessLevel.BEGINNER
        );

        assertThat(longTrack.eligible()).isFalse();
        assertThat(highGainTrack.eligible()).isFalse();
    }

    @Test
    void intermediateLevelRejectsTrackOutsideLimits() throws Exception {
        TrackScorer.TrackEvaluation longTrack = scorer.evaluate(
                course(14000.0, List.of(100.0, 500.0), List.of()),
                FitnessLevel.INTERMEDIATE
        );
        TrackScorer.TrackEvaluation dangerousTrack = scorer.evaluate(
                course(10000.0, List.of(100.0, 500.0), List.of("DANGER", "DANGER", "DANGER", "DANGER")),
                FitnessLevel.INTERMEDIATE
        );

        assertThat(longTrack.eligible()).isFalse();
        assertThat(dangerousTrack.eligible()).isFalse();
    }

    @Test
    void metricsIgnoreNullWaypointsAndNullAltitudes() throws Exception {
        Course course = courseWithWaypoints(
                4000.0,
                java.util.Arrays.asList(null, null),
                java.util.Arrays.asList(
                        null,
                        new Course.CourseWaypoint(37.0, 127.0, 100.0, "위험", "DANGER")
                )
        );

        TrackScorer.TrackEvaluation evaluation = scorer.evaluate(course, FitnessLevel.ENTRY);

        assertThat(evaluation.metrics().gain()).isZero();
        assertThat(evaluation.metrics().mountainHeightM()).isZero();
        assertThat(evaluation.metrics().dangerCount()).isEqualTo(1);
    }

    @Test
    void metricsUsePositiveWaypointElevationsWhenAltitudesAreMissing() throws Exception {
        Course course = courseWithWaypoints(
                6000.0,
                null,
                List.of(
                        new Course.CourseWaypoint(37.0, 127.0, null, "쉼터", "REST"),
                        new Course.CourseWaypoint(37.1, 127.1, 100.0, "입구", "TRANS"),
                        new Course.CourseWaypoint(37.2, 127.2, 450.0, "전망대", "PHOTO")
                )
        );

        TrackScorer.TrackEvaluation evaluation = scorer.evaluate(course, FitnessLevel.BEGINNER);

        assertThat(evaluation.metrics().gain()).isEqualTo(350.0);
        assertThat(evaluation.metrics().mountainHeightM()).isEqualTo(450);
        assertThat(evaluation.metrics().restCount()).isEqualTo(1);
        assertThat(evaluation.metrics().accessCount()).isEqualTo(1);
        assertThat(evaluation.metrics().scenicCount()).isEqualTo(1);
    }

    @Test
    void metricsUseZeroElevationWhenAltitudeAndWaypointElevationsAreMissing() throws Exception {
        Course course = course(3000.0, null, null);

        TrackScorer.TrackEvaluation evaluation = scorer.evaluate(course, FitnessLevel.ENTRY);

        assertThat(evaluation.metrics().gain()).isZero();
        assertThat(evaluation.metrics().mountainHeightM()).isZero();
        assertThat(evaluation.metrics().dangerCount()).isZero();
        assertThat(evaluation.metrics().restCount()).isZero();
    }

    private Course course(Double distance, List<Double> altitudes, List<String> waypointCategories) throws Exception {
        List<Course.CourseWaypoint> waypoints = waypointCategories == null
                ? null
                : waypointCategories.stream()
                .map(category -> new Course.CourseWaypoint(37.0, 127.0, 100.0, category, category))
                .toList();
        return courseWithWaypoints(distance, altitudes, waypoints);
    }

    private Course courseWithWaypoints(
            Double distance,
            List<Double> altitudes,
            List<Course.CourseWaypoint> waypoints
    ) throws Exception {
        Constructor<Course> constructor = Course.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Course course = constructor.newInstance();

        ReflectionTestUtils.setField(course, "distance", distance);
        ReflectionTestUtils.setField(course, "altitudes", altitudes);
        ReflectionTestUtils.setField(course, "waypoints", waypoints);
        return course;
    }
}
