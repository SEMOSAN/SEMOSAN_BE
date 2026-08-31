-- ttlSeconds 는 "현재 윈도우가 끝날 때까지 남은 시간" (윈도우 전체 길이 아님).
-- 매 호출마다 EXPIRE 를 다시 걸어도 절대 만료 시각은 항상 윈도우 끝 지점으로 수렴하므로 안전하다.
-- 이 방식은 이전 배포에서 EXPIRE 가 유실돼 TTL 없이 남은 키도 다음 요청에서 자동으로 복구한다.
local key = KEYS[1]
local ttlSeconds = tonumber(ARGV[1])

local count = redis.call("INCR", key)
redis.call("EXPIRE", key, ttlSeconds)

return count
