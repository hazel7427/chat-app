-- KEYS[1] = lastReadMessageKey
-- KEYS[2] = messageZSetKey
-- KEYS[3] = unreadCountHashKey

local lastReadMessageKey = KEYS[1]
local messageZSetKey = KEYS[2]
local unreadCountHashKey = KEYS[3]

-- 최신 메시지 ID 가져오기
local latestMessageId = redis.call('ZRANGE', messageZSetKey, -1, -1)[1]
if not latestMessageId then
    return { -1, -1 }
end

local currentLastReadRaw = redis.call('GET', lastReadMessageKey)
local currentLastReadId = tonumber(currentLastReadRaw) or -1
local newLastReadId = tonumber(latestMessageId) or -1

-- 순위 계산
local startRank = redis.call('ZRANK', messageZSetKey, tostring(currentLastReadId))
local endRank = redis.call('ZRANK', messageZSetKey, tostring(newLastReadId))

if startRank and endRank and endRank > startRank then
    local midMessages = redis.call('ZRANGE', messageZSetKey, startRank + 1, endRank)
    for _, mid in ipairs(midMessages or {}) do
        redis.call('HINCRBY', unreadCountHashKey, mid, -1)
    end
end

redis.call('SET', lastReadMessageKey, tostring(newLastReadId))
return { currentLastReadId, newLastReadId }
