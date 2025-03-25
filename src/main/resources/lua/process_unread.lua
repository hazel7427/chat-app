local lastReadMessageKey = KEYS[1]
local messageZSetKey = KEYS[2]
local unreadCountHashKey = KEYS[3]

-- 최신 메시지 ID 가져오기
local latestMessageId = redis.call('ZRANGE', messageZSetKey, -1, -1)[1]
if not latestMessageId then
    return 0
end

local currentLastReadId = tonumber(redis.call('GET', lastReadMessageKey) or '-1')
local newLastReadId = tonumber(latestMessageId)

if newLastReadId <= currentLastReadId then
    return 0
end

-- 읽지 않은 메시지 목록 가져오기 & unreadCount 감소
local unreadMessages = redis.call('ZRANGEBYSCORE', messageZSetKey, currentLastReadId + 1, newLastReadId)
for _, messageId in ipairs(unreadMessages) do
    redis.call('HINCRBY', unreadCountHashKey, messageId, -1)
end

redis.call('SET', lastReadMessageKey, newLastReadId)
return newLastReadId
