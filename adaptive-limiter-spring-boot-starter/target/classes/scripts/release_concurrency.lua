-- =========================================================================
-- Redis Lua Script: Decrement In-Flight Concurrency upon request completion
-- =========================================================================
local concurrency_key = KEYS[1]
local current = tonumber(redis.call('GET', concurrency_key) or "0")

if current > 0 then
    redis.call('DECR', concurrency_key)
end

return 1
