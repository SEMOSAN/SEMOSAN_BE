-- =====================================================================
-- V37__backfill_course_summit.sql
-- 목적: 코스별 정상 좌표(summit_lat/lng/ele)를 경로 데이터로 일괄 백필한다.
--       정상 = 경로(polyline)에서 고도(altitudes)가 가장 높은 지점.
--       altitudes[i] 는 polyline 의 i 번째 점과 1:1 대응하므로,
--       최고 고도의 ordinality 로 ST_PointN 을 뽑아 좌표를 얻는다.
--
-- 보존 규칙: summit_lat 이 이미 채워진 코스(관리자가 수동 지정)는 건너뛴다.
-- 대상 제외: polyline/altitudes 가 없거나, 둘의 점 개수가 어긋난 코스.
-- 동점 처리: 같은 최고 고도가 여러 지점이면 더 앞선(먼저 나온) 지점을 택한다.
-- =====================================================================

WITH peak AS (
    SELECT DISTINCT ON (c.id)
        c.id                        AS course_id,
        a.ordinality::int           AS idx,
        (a.value #>> '{}')::numeric AS alt
    FROM courses c
    CROSS JOIN LATERAL
        jsonb_array_elements(c.altitudes) WITH ORDINALITY AS a(value, ordinality)
    WHERE c.summit_lat IS NULL
      AND c.polyline IS NOT NULL
      AND c.altitudes IS NOT NULL
      AND jsonb_array_length(c.altitudes) > 0
      AND ST_NPoints(c.polyline::geometry) = jsonb_array_length(c.altitudes)
    ORDER BY c.id, (a.value #>> '{}')::numeric DESC, a.ordinality ASC
)
UPDATE courses c
SET summit_lat = ST_Y(ST_PointN(c.polyline::geometry, p.idx)),
    summit_lng = ST_X(ST_PointN(c.polyline::geometry, p.idx)),
    summit_ele = p.alt
FROM peak p
WHERE c.id = p.course_id;
