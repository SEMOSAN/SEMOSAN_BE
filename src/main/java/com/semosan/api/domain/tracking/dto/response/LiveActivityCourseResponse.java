package com.semosan.api.domain.tracking.dto.response;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Course;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.Arrays;
import java.util.List;

/**
 * summitDistance / summitEstimatedTime 은 사진 마일스톤이 정상 기준으로 잡히는 것과 같은 값을 쓴다.
 * 화면에 표시되는 "정상까지 거리/시간" 과 실제 푸시가 오는 지점이 어긋나면 안 되기 때문이다.
 */
public record LiveActivityCourseResponse(
        Long courseId,
        List<CoordinateInfo> coordinates,
        Double totalDistance,
        Integer estimatedTime,
        /** 시작점→정상 누적 거리(m). 정상 좌표가 없어 계산 불가한 코스는 null. */
        Double summitDistance,
        /** 정상까지 예상 소요 시간(분). summitDistance 가 null 이면 null. */
        Integer summitEstimatedTime
) {

    public static LiveActivityCourseResponse from(Course course, Double summitDistance) {
        return new LiveActivityCourseResponse(
                course.getId(),
                toCoordinates(course.getPolyline()),
                course.getDistance(),
                course.getDuration(),
                summitDistance,
                estimateSummitTime(course, summitDistance)
        );
    }

    /**
     * 정상까지 예상 시간 = 코스 전체 소요 시간 × (정상까지 거리 / 코스 전체 거리).
     *
     * 오르막이 평지보다 느리다는 점을 반영하지 못하는 근사값이지만, 정상을 코스 중간으로 가정하던
     * 것보다는 실제에 가깝다. 비율은 1.0 으로 상한을 둔다 — 정상까지 거리는 polyline 을 Haversine 으로
     * 누적한 값이고 course.distance 는 별도 출처라, 정상이 코스 끝인 경우 미세하게 넘길 수 있다.
     */
    private static Integer estimateSummitTime(Course course, Double summitDistance) {
        Double totalDistance = course.getDistance();
        Integer duration = course.getDuration();
        if (summitDistance == null || duration == null || totalDistance == null || totalDistance <= 0) {
            return null;
        }
        return (int) Math.round(duration * Math.min(summitDistance / totalDistance, 1.0));
    }

    private static List<CoordinateInfo> toCoordinates(LineString polyline) {
        if (polyline == null || polyline.isEmpty()) {
            throw new GeneralException(ErrorStatus.TRACKING_COURSE_POLYLINE_REQUIRED);
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
