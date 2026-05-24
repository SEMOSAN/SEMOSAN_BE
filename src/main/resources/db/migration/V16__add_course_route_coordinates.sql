-- Live Activity에서 코스 기반 진행률/남은 거리 계산에 사용할 전체 코스 좌표 배열.
-- 형식: [{"latitude": 37.123456, "longitude": 127.123456}, ...]
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS route_coordinates jsonb NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN courses.route_coordinates IS
    'Course route GPS coordinates for Live Activity. Array of latitude/longitude objects.';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'courses_route_coordinates_array_check'
    ) THEN
        ALTER TABLE courses
            ADD CONSTRAINT courses_route_coordinates_array_check
                CHECK (jsonb_typeof(route_coordinates) = 'array');
    END IF;
END $$;
