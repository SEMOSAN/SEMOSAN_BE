-- 코스 누적 상승/하강 고도 + 최고 고도 컬럼 추가.
-- altitudes(jsonb 숫자 배열) 를 풀어 다음 정의로 백필한다.
--   ascent       = Σ max(0, current - previous)
--   descent      = Σ max(0, previous - current)
--   max_altitude = max(altitudes)
-- altitudes 가 null 이거나 점이 1개 이하인 코스는 셋 다 NULL 로 남김.

ALTER TABLE courses
    ADD COLUMN ascent       DOUBLE PRECISION,
    ADD COLUMN descent      DOUBLE PRECISION,
    ADD COLUMN max_altitude DOUBLE PRECISION;

WITH altitude_pairs AS (
    SELECT
        c.id AS course_id,
        (a.value #>> '{}')::numeric AS altitude,
        LAG((a.value #>> '{}')::numeric)
            OVER (PARTITION BY c.id ORDER BY a.ordinality) AS prev_altitude
    FROM courses c
    CROSS JOIN LATERAL
        jsonb_array_elements(c.altitudes) WITH ORDINALITY AS a(value, ordinality)
    WHERE c.altitudes IS NOT NULL
      AND jsonb_array_length(c.altitudes) > 1
),
calculated AS (
    -- prev_altitude IS NULL 인 첫 점도 포함시켜 MAX(altitude) 가 모든 점을 보게 한다.
    -- ascent/descent 의 GREATEST(0, x - NULL) 은 PostgreSQL 이 NULL 을 무시해 0 으로 처리하므로 SUM 결과에 영향 없음.
    SELECT
        course_id,
        SUM(GREATEST(0, altitude - prev_altitude)) AS ascent,
        SUM(GREATEST(0, prev_altitude - altitude)) AS descent,
        MAX(altitude)                              AS max_altitude
    FROM altitude_pairs
    GROUP BY course_id
)
UPDATE courses c
SET ascent       = calc.ascent,
    descent      = calc.descent,
    max_altitude = calc.max_altitude
FROM calculated calc
WHERE c.id = calc.course_id;
