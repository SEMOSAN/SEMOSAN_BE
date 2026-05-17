-- =====================================================================
-- 001_postgis_setup.sql
-- 목적: 트래킹 기능을 위한 PostGIS 도입 및 기존 산 좌표 데이터 마이그레이션
-- 적용 시점: 1회성. 본 변경이 develop/main 에 반영된 직후 운영 DB 에도 적용.
-- 사전 조건:
--   - PostgreSQL 컨테이너가 PostGIS extension 이 설치된 이미지여야 함
--     (예: postgis/postgis:16-3.4)
--   - 일반 postgres 이미지는 CREATE EXTENSION 시 ERROR 발생
-- 적용 환경:
--   - local : 본 SQL 그대로 실행
--   - prod (k8s) : Postgres 이미지를 postgis/postgis 계열로 교체 후 동일 SQL 실행
-- =====================================================================

-- 1) PostGIS extension 활성화 (idempotent)
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2) mountains 테이블에 공간 좌표 컬럼 추가 (idempotent)
--    ddl-auto=update 환경에서는 Hibernate 가 자동 추가하지만,
--    운영(ddl-auto=validate/none)에서는 본 SQL 로 명시 적용 필요.
ALTER TABLE mountains
    ADD COLUMN IF NOT EXISTS location geography(Point, 4326);

-- 3) 기존 latitude/longitude → location 백필 (idempotent)
UPDATE mountains
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE location IS NULL
  AND latitude IS NOT NULL
  AND longitude IS NOT NULL;

-- 4) 공간 인덱스 (nearest / 거리 쿼리 성능)
CREATE INDEX IF NOT EXISTS idx_mountains_location
    ON mountains
    USING GIST (location);
