package com.semosan.api.domain.tracking.dto.response;

import com.semosan.api.domain.mountain.entity.Course;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.Arrays;
import java.util.List;

public record LiveActivityCourseResponse(
        Long courseId,
        List<CoordinateInfo> coordinates,
        Double totalDistance,
        Integer estimatedTime
) {

    public static LiveActivityCourseResponse from(Course course) {
        return new LiveActivityCourseResponse(
                course.getId(),
                toCoordinates(course.getPolyline()),
                course.getDistance(),
                course.getDuration()
        );
    }

    private static List<CoordinateInfo> toCoordinates(LineString polyline) {
        if (polyline == null) {
            return List.of();
        }
        return Arrays.stream(polyline.getCoordinates())
                .map(CoordinateInfo::from)
                .toList();
    }

    public record CoordinateInfo(
            Double latitude,
            Double longitude
    ) {
        public static CoordinateInfo from(Coordinate coordinate) {
            return new CoordinateInfo(
                    coordinate.y,
                    coordinate.x
            );
        }
    }
}
