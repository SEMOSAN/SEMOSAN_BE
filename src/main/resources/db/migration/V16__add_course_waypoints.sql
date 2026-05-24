-- KOMOUNT 트랙별 waypoint/category 정보를 저장한다.
-- 형식: [{"lat": 37.123, "lng": 127.123, "ele": 100.0, "name": "정상", "category": "PEAK"}, ...]
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS waypoints jsonb;

COMMENT ON COLUMN courses.waypoints IS
    'KOMOUNT course waypoints. Array of lat/lng/ele/name/category objects.';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'courses_waypoints_array_check'
          AND conrelid = 'courses'::regclass
    ) THEN
        ALTER TABLE courses
            ADD CONSTRAINT courses_waypoints_array_check
                CHECK (waypoints IS NULL OR jsonb_typeof(waypoints) = 'array');
    END IF;
END $$;
