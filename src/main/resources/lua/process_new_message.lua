-- KEYS
-- [1] participantsKey
-- [2] connectedUsersKey
-- [3] unreadCountHashKey
-- [4] lastReadKeyPattern (ex: "chat:last_read:{userId}:{roomId}")
-- [5] messageZSetKey

-- ARGV
-- [1] roomId
-- [2] messageId
-- [3] senderId

local participants = redis.call('SMEMBERS', KEYS[1])
local connectedUsersKey = KEYS[2]
local unreadCountHashKey = KEYS[3]
local lastReadKeyPattern = KEYS[4]
local messageZSetKey = KEYS[5]

local roomId = ARGV[1]
local messageId = tonumber(ARGV[2])
local senderId = ARGV[3]

local unreadCount = 0
local readUsers = {}

for _, participantId in ipairs(participants) do
    local isConnected = redis.call('SISMEMBER', connectedUsersKey, participantId)
    local isSender = (participantId == senderId)

    local lastReadMessageKey = string.gsub(lastReadKeyPattern, "{userId}", participantId)
    lastReadMessageKey = string.gsub(lastReadMessageKey, "{roomId}", roomId)

    local lastReadRaw = redis.call('GET', lastReadMessageKey)
    local lastReadId = tonumber(lastReadRaw)
    if lastReadId == nil then
        lastReadId = -1
    end

    if isSender or isConnected == 1 then
        -- 자동 읽음 처리
        if lastReadId < messageId then
            redis.call('SET', lastReadMessageKey, messageId)
            table.insert(readUsers, participantId)

            local unreadMessages = redis.call('ZRANGEBYSCORE', messageZSetKey, lastReadId + 1, messageId - 1)
            for _, mid in ipairs(unreadMessages or {}) do
                redis.call('HINCRBY', unreadCountHashKey, mid, -1)
            end
        end
    else
        -- 읽지 않은 유저
        unreadCount = unreadCount + 1
    end
end

-- 현재 메시지 unreadCount 저장
redis.call('HSET', unreadCountHashKey, messageId, unreadCount)

-- 결과 반환: {unreadCount, 읽은 유저 ID 리스트}
return { unreadCount, readUsers }
