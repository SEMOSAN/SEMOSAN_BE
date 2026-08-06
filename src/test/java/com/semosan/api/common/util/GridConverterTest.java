package com.semosan.api.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GridConverterTest {

    @Test
    void toGridConvertsSeoulCoordinatesToKmaGrid() {
        GridConverter.Grid grid = GridConverter.toGrid(37.5665, 126.9780);

        assertThat(grid.nx()).isEqualTo(60);
        assertThat(grid.ny()).isEqualTo(127);
    }

    @Test
    void toGridConvertsJejuCoordinatesToKmaGrid() {
        GridConverter.Grid grid = GridConverter.toGrid(33.4996, 126.5312);

        assertThat(grid.nx()).isEqualTo(53);
        assertThat(grid.ny()).isEqualTo(38);
    }

    @Test
    void toGridNormalizesLongitudeGreaterThanPiFromOrigin() {
        GridConverter.Grid grid = GridConverter.toGrid(37.0, 400.0);

        assertThat(grid.nx()).isEqualTo(-1178);
    }

    @Test
    void toGridNormalizesLongitudeLessThanMinusPiFromOrigin() {
        GridConverter.Grid grid = GridConverter.toGrid(37.0, -200.0);

        assertThat(grid.nx()).isEqualTo(615);
    }
}
