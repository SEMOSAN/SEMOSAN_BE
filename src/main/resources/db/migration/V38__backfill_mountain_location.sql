-- =====================================================================
-- V38__backfill_mountain_location.sql
-- 목적: location 이 NULL 인 산을 latitude/longitude 기준으로 백필.
--
-- 배경: V5 가 location 컬럼을 추가하며 백필했지만, 그 시점에 존재하던 산은
--       V3 의 7개(관악·북한·도봉·청계·수락·인왕·아차) 뿐이었다.
--       이후 V12 가 산림청 명산 97개를 INSERT 할 때 컬럼 목록에 location 이
--       없어 전부 NULL 로 들어갔고, V5 백필은 이미 지나가 다시 돌지 않았다.
--       그 결과 MountainRepository.findNearestByLatLng 의
--       `WHERE location IS NOT NULL` 필터에 V12 산이 전부 걸러져,
--       전국 어디서 호출해도 수도권 산만 반환됐다.
--
-- 재발 방지(location 을 생성 컬럼으로 전환)는 V39 에서 다룬다.
-- 이 마이그레이션은 그 전환이 실패하더라도 증상만은 해소된 상태로 남기기 위해
-- 의도적으로 분리했다.
-- 멱등: location 이 이미 채워진 행은 건드리지 않는다.
-- =====================================================================
UPDATE mountains
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE location IS NULL
  AND latitude IS NOT NULL
  AND longitude IS NOT NULL;
