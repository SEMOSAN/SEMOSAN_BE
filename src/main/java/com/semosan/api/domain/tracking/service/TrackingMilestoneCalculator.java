package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 트래킹 세션의 사진 촬영 마일스톤 거리와 정상 알림 임계 거리를 계산한다.
 *
 *  - 코스 + 정상 좌표 있음: 시작점→정상 누적 거리의 1/4, 2/4, 3/4, 4/4 지점 (총 4컷).
 *    4/4 가 곧 정상이므로 summitMark 도 같은 값이다. 정상까지 푸시가 4번 오고 하산 구간엔 없다.
 *  - 코스 + 정상 좌표 없음: course.distance 4등분으로 fallback.
 *    summitMark 는 종전 정책대로 코스 거리의 절반이다.
 *  - 자유 기록: 500m 간격 4컷 (500/1000/1500/2000m). 정상 개념이 없어 summitMark 는 null.
 *
 * 단위 정책: 입력/출력 모두 미터(m). distanceTotal(Haversine 누적, m) 과 비교될 값이라 단위 일치 필수.
 * 과거: course.distance 를 km 로 가정하고 × 1000 했으나, DB/시드/응답이 모두 m 단위라 마일스톤이 1000배로 박혀 OPEN 이 영영 안 오는 버그가 있었음.
 */
@Component
public class TrackingMilestoneCalculator {

    private static final int COURSE_MILESTONE_COUNT = 4;
    private static final double FREE_RECORDING_INTERVAL_METERS = 500.0;
    private static final int FREE_RECORDING_MAX_COUNT = 4;

    /** redis/tracking-stats-update.lua 가 distanceTotal 을 누적할 때 쓰는 값과 동일해야 두 거리가 비교 가능하다. */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public MilestonePlan calculate(TrackingSession session) {
        if (Boolean.TRUE.equals(session.getIsFreeRecording()) || session.getCourse() == null) {
            return new MilestonePlan(freeRecordingMilestones(), null);
        }
        return courseMilestones(session.getCourse());
    }

    /**
     * @param milestones 사진 촬영 마일스톤 거리 목록 (m)
     * @param summitMark 정상 알림 임계 거리 (m). 자유 기록이면 null 이며, 이 null 여부가 곧 코스 모드 판별 기준이다.
     *                   자유 기록도 4컷이라 마일스톤 개수로는 코스와 구분되지 않기 때문.
     */
    public record MilestonePlan(List<Double> milestones, Double summitMark) {
    }

    private MilestonePlan courseMilestones(Course course) {
        Double summitDistance = distanceToSummit(course);
        if (summitDistance != null && summitDistance > 0) {
            return new MilestonePlan(split(summitDistance), summitDistance);
        }
        // 정상 좌표나 polyline 이 없는 코스 — 종전 정책(코스 전체 4등분 + 절반 지점 정상) 유지.
        double totalMeters = course.getDistance() == null ? 0.0 : course.getDistance();
        return new MilestonePlan(split(totalMeters), totalMeters / 2.0);
    }

    private static List<Double> split(double totalMeters) {
        List<Double> result = new ArrayList<>(COURSE_MILESTONE_COUNT);
        for (int i = 1; i <= COURSE_MILESTONE_COUNT; i++) {
            result.add(totalMeters * i / COURSE_MILESTONE_COUNT);
        }
        return result;
    }

    /**
     * 코스 시작점부터 정상 지점까지 polyline 을 따라간 누적 거리(m).
     *
     * V37 백필로 채워진 정상 좌표는 polyline 위의 점과 정확히 일치하지만, 관리자가 waypoint 로
     * 수동 지정(AdminMountainService#updateSummit)한 좌표는 polyline 밖의 점일 수 있다.
     * 두 경우를 모두 커버하려고 정확 일치가 아닌 최근접 점 탐색으로 인덱스를 찾는다.
     *
     * @return 계산 불가하면 null — 정상 좌표 없음 / polyline 없음 / 점 2개 미만 / 정상이 시작점인 경우.
     *         정상이 시작점(idx 0)이면 4등분해도 마일스톤이 전부 0 이라 의미가 없어 fallback 시킨다.
     */
    private Double distanceToSummit(Course course) {
        Double summitLat = course.getSummitLat();
        Double summitLng = course.getSummitLng();
        LineString polyline = course.getPolyline();
        if (summitLat == null || summitLng == null || polyline == null) {
            return null;
        }
        Coordinate[] coords = polyline.getCoordinates();
        if (coords.length < 2) {
            return null;
        }

        // JTS Coordinate 는 x=경도, y=위도 순서다.
        int nearestIdx = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < coords.length; i++) {
            double distance = haversineMeters(coords[i].y, coords[i].x, summitLat, summitLng);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIdx = i;
            }
        }
        if (nearestIdx == 0) {
            return null;
        }

        double cumulative = 0.0;
        for (int i = 1; i <= nearestIdx; i++) {
            cumulative += haversineMeters(coords[i - 1].y, coords[i - 1].x, coords[i].y, coords[i].x);
        }
        return cumulative;
    }

    private List<Double> freeRecordingMilestones() {
        List<Double> result = new ArrayList<>(FREE_RECORDING_MAX_COUNT);
        for (int i = 1; i <= FREE_RECORDING_MAX_COUNT; i++) {
            result.add(FREE_RECORDING_INTERVAL_METERS * i);
        }
        return result;
    }

    /** redis/tracking-stats-update.lua 의 누적 공식과 동일 — 두 거리가 같은 기준이어야 마일스톤이 맞는다. */
    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double rad = Math.PI / 180;
        double dLat = (lat2 - lat1) * rad;
        double dLng = (lng2 - lng1) * rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1 * rad) * Math.cos(lat2 * rad)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
