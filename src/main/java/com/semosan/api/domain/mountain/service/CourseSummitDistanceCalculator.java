package com.semosan.api.domain.mountain.service;

import com.semosan.api.domain.mountain.entity.Course;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;

/**
 * 코스 시작점부터 정상 지점까지 polyline 을 따라간 누적 거리(m)를 계산한다.
 *
 * V37 백필로 채워진 정상 좌표는 polyline 위의 점과 정확히 일치하지만, 관리자가 waypoint 로
 * 수동 지정(AdminMountainService#updateSummit)한 좌표는 polyline 밖의 점일 수 있다.
 * 두 경우를 모두 커버하려고 정확 일치가 아닌 최근접 점 탐색으로 인덱스를 찾는다.
 *
 * 트래킹 사진 마일스톤 계산과 Live Activity 코스 응답이 반드시 같은 값을 써야 해서
 * (푸시가 오는 지점과 화면에 표시되는 "정상까지 거리" 가 어긋나면 안 된다) 별도 컴포넌트로 분리했다.
 */
@Component
public class CourseSummitDistanceCalculator {

    /** redis/tracking-stats-update.lua 가 distanceTotal 을 누적할 때 쓰는 값과 동일해야 두 거리가 비교 가능하다. */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * @return 정상까지 누적 거리(m). 계산 불가하면 null — 정상 좌표 없음 / polyline 없음 /
     *         점 2개 미만 / 정상이 코스 시작점인 경우.
     *         정상이 시작점(idx 0)이면 4등분해도 마일스톤이 전부 0 이라 의미가 없어 호출자가 fallback 하도록 null 을 준다.
     */
    public Double calculate(Course course) {
        if (course == null) {
            return null;
        }
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

        int nearestIdx = nearestPointIndex(coords, summitLat, summitLng);
        if (nearestIdx == 0) {
            return null;
        }

        double cumulative = 0.0;
        for (int i = 1; i <= nearestIdx; i++) {
            cumulative += haversineMeters(coords[i - 1].y, coords[i - 1].x, coords[i].y, coords[i].x);
        }
        return cumulative;
    }

    /** JTS Coordinate 는 x=경도, y=위도 순서다. */
    private static int nearestPointIndex(Coordinate[] coords, double summitLat, double summitLng) {
        int nearestIdx = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < coords.length; i++) {
            double distance = haversineMeters(coords[i].y, coords[i].x, summitLat, summitLng);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIdx = i;
            }
        }
        return nearestIdx;
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
