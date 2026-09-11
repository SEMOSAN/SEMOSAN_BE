package com.semosan.api.common.util;

import org.locationtech.jts.geom.Coordinate;

/**
 * 위경도 두 점 사이의 지표면 거리(m)를 구한다.
 *
 * 트래킹 누적 거리(distanceTotal)를 계산하는 {@code redis/tracking-stats-update.lua} 와
 * 반드시 같은 공식·같은 지구 반지름을 써야 한다. 사진 마일스톤과 정상 알림이 이 두 값을
 * 비교해 발송 시점을 정하므로, 기준이 어긋나면 푸시가 엉뚱한 지점에서 나간다.
 *
 * TODO: {@code CourseSlopeSegmentCalculator} 와 {@code TrackingStreamConsumer} 에도
 *       같은 공식이 사본으로 남아 있다. 별도 작업에서 이쪽으로 통합할 것.
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoDistance() {
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double rad = Math.PI / 180;
        double dLat = (lat2 - lat1) * rad;
        double dLng = (lng2 - lng1) * rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1 * rad) * Math.cos(lat2 * rad)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * 좌표 배열을 순서대로 이은 경로의 총 길이(m).
     *
     * @param coordinates JTS 좌표 배열 — x=경도, y=위도 순서다.
     * @return 점이 2개 미만이면 0
     */
    public static double pathLengthMeters(Coordinate[] coordinates) {
        if (coordinates == null || coordinates.length < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 1; i < coordinates.length; i++) {
            total += haversineMeters(
                    coordinates[i - 1].y, coordinates[i - 1].x,
                    coordinates[i].y, coordinates[i].x
            );
        }
        return total;
    }
}
