package com.semosan.api.domain.admin.dto.response;

import com.semosan.api.domain.mountain.entity.Course;

import java.util.List;

public record AdminCourseWaypointsResponse(
        Long courseId,
        String courseName,
        Double summitLat,
        Double summitLng,
        Double summitEle,
        List<WaypointInfo> waypoints
) {

    public static AdminCourseWaypointsResponse from(Course course) {
        List<WaypointInfo> waypoints = course.getWaypoints() == null
                ? List.of()
                : course.getWaypoints().stream().map(WaypointInfo::from).toList();
        return new AdminCourseWaypointsResponse(course.getId(), course.getName(),
                course.getSummitLat(), course.getSummitLng(), course.getSummitEle(), waypoints);
    }

    public record WaypointInfo(
            Double lat,
            Double lng,
            Double ele,
            String name,
            String category
    ) {
        public static WaypointInfo from(Course.CourseWaypoint waypoint) {
            return new WaypointInfo(
                    waypoint.lat(),
                    waypoint.lng(),
                    waypoint.ele(),
                    waypoint.name(),
                    waypoint.category()
            );
        }
    }
}
