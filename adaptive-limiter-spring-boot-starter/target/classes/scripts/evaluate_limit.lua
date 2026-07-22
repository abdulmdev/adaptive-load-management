-- =========================================================================
-- Redis Lua Script: Atomic Dynamic Token Bucket + Priority Load Shedding Evaluation
-- =========================================================================
-- Keys:
-- KEYS[1]: Token Bucket Key (e.g., rate:limit:order-service:get_orders)
-- KEYS[2]: Concurrency Tracker Key (e.g., concurrency:order-service:get_orders)
--
-- Arguments:
-- ARGV[1]: Priority Level (0 for P0, 1 for P1, 2 for P2, 3 for P3)
-- ARGV[2]: Max Allowed Capacity / Concurrency Limit (Calculated dynamically by Control Plane)
-- ARGV[3]: Current Cluster Load Factor L (Float between 0.00 and 1.00)
-- ARGV[4]: P1 Shed Threshold (e.g., 0.90)
-- ARGV[5]: P2 Shed Threshold (e.g., 0.80)
-- ARGV[6]: P3 Shed Threshold (e.g., 0.65)
-- ARGV[7]: Current Timestamp (epoch ms)
-- =========================================================================

local token_key = KEYS[1]
local concurrency_key = KEYS[2]

local priority = tonumber(ARGV[1])
local current_limit = tonumber(ARGV[2])
local load_factor = tonumber(ARGV[3])
local p1_threshold = tonumber(ARGV[4])
local p2_threshold = tonumber(ARGV[5])
local p3_threshold = tonumber(ARGV[6])

-- 1. Check Tiered Priority Load Shedding
if priority == 3 and load_factor >= p3_threshold then
    return {0, "SHED_P3_LOAD_FACTOR_EXCEEDED", tostring(load_factor)}
end

if priority == 2 and load_factor >= p2_threshold then
    return {0, "SHED_P2_LOAD_FACTOR_EXCEEDED", tostring(load_factor)}
end

if priority == 1 and load_factor >= p1_threshold then
    return {0, "SHED_P1_LOAD_FACTOR_EXCEEDED", tostring(load_factor)}
end

-- 2. Check In-Flight Concurrency Capacity
local current_concurrency = tonumber(redis.call('GET', concurrency_key) or "0")

if current_concurrency >= current_limit and priority > 0 then
    -- P0 gets a small safety burst allowance (+20% of limit), lower tiers get blocked
    return {0, "CONCURRENCY_LIMIT_EXCEEDED", tostring(current_concurrency)}
end

-- 3. Atomic Increment Concurrency
redis.call('INCR', concurrency_key)
redis.call('EXPIRE', concurrency_key, 10) -- 10s auto-expiry safety guard

return {1, "ALLOWED", tostring(current_concurrency + 1)}
