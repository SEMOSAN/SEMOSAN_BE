package com.semosan.api.common.util;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoDistanceTest {

    /** 위도 0.001도 ≈ 111.19m (R=6,371,000m 기준). */
    private static final double METERS_PER_MILLI_DEGREE_LAT = 111.1949;

    @Test
    void haversineMetersReturnsZeroForSamePoint() {
        assertThat(GeoDistance.haversineMeters(37.5, 127.0, 37.5, 127.0)).isZero();
    }

    @Test
    void haversineMetersMatchesKnownMeridianDistance() {
        double distance = GeoDistance.haversineMeters(37.0, 127.0, 37.001, 127.0);

        assertThat(distance).isCloseTo(METERS_PER_MILLI_DEGREE_LAT, within(0.2));
    }

    @Test
    void haversineMetersIsSymmetric() {
        double forward = GeoDistance.haversineMeters(37.0, 127.0, 37.5, 127.5);
        double backward = GeoDistance.haversineMeters(37.5, 127.5, 37.0, 127.0);

        assertThat(forward).isEqualTo(backward);
    }

    @Test
    void pathLengthMetersSumsEverySegment() {
        // JTS Coordinate 는 x=경도, y=위도 순서다.
        Coordinate[] path = {
                new Coordinate(127.0, 37.0),
                new Coordinate(127.0, 37.001),
                new Coordinate(127.0, 37.002),
                new Coordinate(127.0, 37.003),
        };

        assertThat(GeoDistance.pathLengthMeters(path))
                .isCloseTo(3 * METERS_PER_MILLI_DEGREE_LAT, within(0.5));
    }

    @Test
    void pathLengthMetersReturnsZeroWhenFewerThanTwoPoints() {
        assertThat(GeoDistance.pathLengthMeters(null)).isZero();
        assertThat(GeoDistance.pathLengthMeters(new Coordinate[0])).isZero();
        assertThat(GeoDistance.pathLengthMeters(new Coordinate[]{new Coordinate(127.0, 37.0)})).isZero();
    }
}
