local key = KEYS[1]
local ttlSeconds = tonumber(ARGV[1])

local count = redis.call("INCR", key)
if count == 1 then
    redis.call("EXPIRE", key, ttlSeconds)
end

return count
