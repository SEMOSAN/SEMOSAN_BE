-- =====================================================================
-- V5__postgis_setup.sql
-- 목적: 트래킹 기능(가까운 산 검색)을 위한 PostGIS 도입 및 좌표 백필
-- 사전 조건: PostgreSQL 컨테이너가 PostGIS extension 이 설치된 이미지여야 함
--            (예: postgis/postgis:16-3.4). 일반 postgres 이미지는 ERROR.
-- =====================================================================

-- 1) PostGIS extension 활성화
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2) mountains 테이블에 공간 좌표 컬럼 추가
ALTER TABLE mountains
    ADD COLUMN IF NOT EXISTS location geography(Point, 4326);

-- 3) 기존 latitude/longitude → location 백필
UPDATE mountains
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE location IS NULL
  AND latitude IS NOT NULL
  AND longitude IS NOT NULL;

-- 4) 공간 인덱스 (nearest / 거리 쿼리 성능)
CREATE INDEX IF NOT EXISTS idx_mountains_location
    ON mountains
    USING GIST (location);
