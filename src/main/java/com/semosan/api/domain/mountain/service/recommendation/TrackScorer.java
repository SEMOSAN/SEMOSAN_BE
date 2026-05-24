package com.semosan.api.domain.mountain.service.recommendation;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.enums.FitnessLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class TrackScorer {

    private static final Set<String> SCENIC_CATEGORIES = Set.of("VIEW", "PHOTO", "PEAK");
    private static final Set<String> ACCESS_CATEGORIES = Set.of("TRANS", "PARK");

    public TrackEvaluation evaluate(Course course, FitnessLevel level) {
        TrackMetrics metrics = TrackMetrics.from(course);
        double score = score(metrics, level);
        return new TrackEvaluation(
                course,
                metrics,
                isEligible(metrics, level),
                score,
                score - metrics.dangerCount() * 20
        );
    }

    private boolean isEligible(TrackMetrics metrics, FitnessLevel level) {
        return switch (level) {
            case ENTRY -> metrics.distanceKm() <= 6
                    && metrics.gain() <= 400
                    && metrics.dangerCount() == 0;
            case BEGINNER -> metrics.distanceKm() <= 9
                    && metrics.gain() <= 600
                    && metrics.dangerCount() <= 1;
            case INTERMEDIATE -> metrics.distanceKm() <= 13
                    && metrics.gain() <= 900
                    && metrics.dangerCount() <= 3;
            case ADVANCED -> true;
        };
    }

    private double score(TrackMetrics metrics, FitnessLevel level) {
        return switch (level) {
            case ENTRY -> -metrics.distanceKm() * 3
                    - metrics.gain() / 20
                    - metrics.dangerCount() * 50
                    + metrics.restCount() * 4
                    + metrics.accessCount() * 6
                    + metrics.scenicCount() * 2;
            case BEGINNER -> -metrics.distanceKm() * 2
                    - metrics.gain() / 30
                    - metrics.dangerCount() * 30
                    + metrics.restCount() * 3
                    + metrics.accessCount() * 5
                    + metrics.scenicCount() * 3;
            case INTERMEDIATE -> -Math.abs(metrics.distanceKm() - 10) * 2
                    - Math.abs(metrics.gain() - 700) / 35
                    - metrics.dangerCount() * 10
                    + metrics.restCount() * 2
                    + metrics.accessCount() * 2
                    + metrics.scenicCount() * 4;
            case ADVANCED -> metrics.distanceKm() * 2
                    + metrics.gain() / 35
                    - metrics.dangerCount() * 4
                    + metrics.restCount()
                    + metrics.scenicCount() * 4;
        };
    }

    public record TrackEvaluation(
            Course course,
            TrackMetrics metrics,
            boolean eligible,
            double score,
            double fallbackScore
    ) {
    }

    public record TrackMetrics(
            double distanceKm,
            double gain,
            int mountainHeightM,
            long dangerCount,
            long scenicCount,
            long restCount,
            long accessCount
    ) {

        private static TrackMetrics from(Course course) {
            ElevationRange elevationRange = ElevationRange.from(course);
            List<Course.CourseWaypoint> waypoints = course.getWaypoints() == null
                    ? List.of()
                    : course.getWaypoints();

            return new TrackMetrics(
                    course.getDistance() / 1000,
                    elevationRange.gain(),
                    (int) Math.round(elevationRange.maxElevation()),
                    countCategory(waypoints, "DANGER"),
                    countCategories(waypoints, SCENIC_CATEGORIES),
                    countCategory(waypoints, "REST"),
                    countCategories(waypoints, ACCESS_CATEGORIES)
            );
        }

        private static long countCategory(List<Course.CourseWaypoint> waypoints, String category) {
            return waypoints.stream()
                    .filter(waypoint -> category.equals(waypoint.category()))
                    .count();
        }

        private static long countCategories(List<Course.CourseWaypoint> waypoints, Set<String> categories) {
            return waypoints.stream()
                    .filter(waypoint -> categories.contains(waypoint.category()))
                    .count();
        }
    }

    private record ElevationRange(double minElevation, double maxElevation) {

        private double gain() {
            return Math.max(0, maxElevation - minElevation);
        }

        private static ElevationRange from(Course course) {
            List<Double> altitudes = course.getAltitudes();
            if (altitudes != null && !altitudes.isEmpty()) {
                return fromNumbers(altitudes);
            }

            List<Double> waypointElevations = course.getWaypoints() == null
                    ? List.of()
                    : course.getWaypoints().stream()
                    .map(Course.CourseWaypoint::ele)
                    .filter(ele -> ele != null && ele > 0)
                    .toList();
            if (!waypointElevations.isEmpty()) {
                return fromNumbers(waypointElevations);
            }

            return new ElevationRange(0, 0);
        }

        private static ElevationRange fromNumbers(List<Double> values) {
            double min = values.stream()
                    .mapToDouble(Double::doubleValue)
                    .min()
                    .orElse(0);
            double max = values.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0);
            return new ElevationRange(min, max);
        }
    }
}
