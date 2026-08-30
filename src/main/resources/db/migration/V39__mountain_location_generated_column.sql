-- =====================================================================
-- V39__mountain_location_generated_column.sql
-- 목적: location 을 latitude/longitude 에서 자동 파생되는 생성 컬럼으로 전환.
--       시드 SQL 이 location 을 빠뜨려도 다시는 NULL 이 생기지 않게 한다.
--       (V38 이 고친 사고의 재발 방지)
--
-- DROP 후 재생성하는 이유: 일반 컬럼을 생성 컬럼으로 바꾸는 ALTER 문법이 없다.
-- 무손실 근거: location 은 latitude/longitude 에서 100% 파생되는 값이라
--              DROP 해도 잃는 정보가 없다. 재생성 시 전 행이 재계산된다.
-- 인덱스: DROP COLUMN 이 idx_mountains_location 을 함께 제거하므로 다시 만든다.
--
-- NULL 처리: ST_MakePoint 는 strict 라 latitude/longitude 중 하나라도 NULL 이면
--            location 도 NULL 이 된다. 기존 쿼리의 `WHERE location IS NOT NULL`
--            가드 의미가 그대로 보존된다.
-- =====================================================================
ALTER TABLE mountains
    DROP COLUMN location;

ALTER TABLE mountains
    ADD COLUMN location geography(Point, 4326)
        GENERATED ALWAYS AS (
            ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
        ) STORED;

CREATE INDEX idx_mountains_location
    ON mountains
    USING GIST (location);
