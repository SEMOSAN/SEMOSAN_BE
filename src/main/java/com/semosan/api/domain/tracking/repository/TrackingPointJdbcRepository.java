package com.semosan.api.domain.tracking.repository;

import com.semosan.api.domain.tracking.dto.command.PendingPointCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TrackingPointJdbcRepository {

    private static final String INSERT_SQL = """
            INSERT INTO tracking_points (
                tracking_session_id,
                location,
                altitude,
                recorded_at,
                created_at,
                updated_at
            )
            VALUES (
                ?,
                ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                ?,
                ?,
                ?,
                ?
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * GPS 점 리스트를 JDBC batch insert 로 일괄 적재한다.
     * PostGIS ST_MakePoint(lng, lat) 함수를 사용해 공간 geography 객체로 직접 변환한다.
     *
     * @param sessionId 세션 ID
     * @param points 저장할 GPS 점 목록
     * @param now 생성/수정 일시
     * @return 저장된 점 수
     */
    public int saveAllInBatch(Long sessionId, List<PendingPointCommand> points, LocalDateTime now) {
        if (points == null || points.isEmpty()) {
            return 0;
        }

        Timestamp nowTimestamp = Timestamp.valueOf(now);

        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PendingPointCommand point = points.get(i);
                ps.setLong(1, sessionId);
                ps.setDouble(2, point.lng());
                ps.setDouble(3, point.lat());

                if (point.altitude() != null) {
                    ps.setDouble(4, point.altitude());
                } else {
                    ps.setNull(4, Types.DOUBLE);
                }

                ps.setTimestamp(5, Timestamp.valueOf(point.recordedAt()));
                ps.setTimestamp(6, nowTimestamp);
                ps.setTimestamp(7, nowTimestamp);
            }

            @Override
            public int getBatchSize() {
                return points.size();
            }
        });

        return points.size();
    }
}
