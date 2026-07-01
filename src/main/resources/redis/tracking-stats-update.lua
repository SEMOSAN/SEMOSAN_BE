local key = KEYS[1]
local lat = tonumber(ARGV[1])
local lng = tonumber(ARGV[2])
local altitude = ARGV[3] ~= "" and tonumber(ARGV[3]) or nil
local recordedAt = ARGV[4]
local ttlSeconds = tonumber(ARGV[5])

local prev_lat = tonumber(redis.call("HGET", key, "last_lat"))
local prev_lng = tonumber(redis.call("HGET", key, "last_lng"))
local prev_altitude = tonumber(redis.call("HGET", key, "last_altitude"))
local distance_total = tonumber(redis.call("HGET", key, "distance_total")) or 0
local ascent_total = tonumber(redis.call("HGET", key, "ascent_total")) or 0
local descent_total = tonumber(redis.call("HGET", key, "descent_total")) or 0
local max_altitude = tonumber(redis.call("HGET", key, "max_altitude"))
local point_count = tonumber(redis.call("HGET", key, "point_count")) or 0

if prev_lat and prev_lng then
    local rad = math.pi / 180
    local dLat = (lat - prev_lat) * rad
    local dLng = (lng - prev_lng) * rad
    local a = math.sin(dLat / 2) * math.sin(dLat / 2)
            + math.cos(prev_lat * rad) * math.cos(lat * rad)
            * math.sin(dLng / 2) * math.sin(dLng / 2)
    local c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    distance_total = distance_total + 6371000.0 * c
end

if altitude and prev_altitude then
    local delta = altitude - prev_altitude
    if delta > 0 then
        ascent_total = ascent_total + delta
    elseif delta < 0 then
        descent_total = descent_total + (-delta)
    end
end

if altitude and (not max_altitude or altitude > max_altitude) then
    max_altitude = altitude
end

point_count = point_count + 1

redis.call("HSET", key,
    "last_lat", tostring(lat),
    "last_lng", tostring(lng),
    "last_recorded_at", recordedAt,
    "distance_total", tostring(distance_total),
    "ascent_total", tostring(ascent_total),
    "descent_total", tostring(descent_total),
    "point_count", tostring(point_count))

if altitude then
    redis.call("HSET", key, "last_altitude", tostring(altitude))
end
if max_altitude then
    redis.call("HSET", key, "max_altitude", tostring(max_altitude))
end

redis.call("EXPIRE", key, ttlSeconds)

return tostring(distance_total)
