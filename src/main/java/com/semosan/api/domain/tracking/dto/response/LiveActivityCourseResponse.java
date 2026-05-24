package com.semosan.api.domain.tracking.dto.response;

import com.semosan.api.domain.mountain.entity.Course;

import java.util.List;

public record LiveActivityCourseResponse(
        Long courseId,
        List<CoordinateInfo> coordinates,
        Double totalDistance,
        Integer estimatedTime
) {

    public static LiveActivityCourseResponse from(Course course) {
        List<Course.RouteCoordinate> routeCoordinates = course.getRouteCoordinates();
        return new LiveActivityCourseResponse(
                course.getId(),
                routeCoordinates == null
                        ? List.of()
                        : routeCoordinates.stream().map(CoordinateInfo::from).toList(),
                course.getDistance(),
                course.getDuration()
        );
    }

    public record CoordinateInfo(
            Double latitude,
            Double longitude
    ) {
        public static CoordinateInfo from(Course.RouteCoordinate coordinate) {
            return new CoordinateInfo(
                    coordinate.latitude(),
                    coordinate.longitude()
            );
        }
    }
}
