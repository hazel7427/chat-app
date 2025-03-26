-- KEYS
-- [1] participantsKey <- 채팅방 참여자 목록 (미리 캐싱되어있어야함)
-- [2] connectedUsersKey 
-- [3] unreadCountHashKey 
-- [4] lastReadKeyPattern (ex: "chat:last_read:{userId}:{roomId}")
-- [5] messageZSetKey <- 채팅방 메시지들 목록 (미리 캐싱되어 있어야함)

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

-- 채팅방 참여자 목록 조회
for _, participantId in ipairs(participants) do
    local isConnected = redis.call('SISMEMBER', connectedUsersKey, participantId)
    local isSender = (tostring(participantId) == tostring(senderId))

    -- 유저별 lastReadId 조회
    local lastReadMessageKey = string.gsub(lastReadKeyPattern, "{userId}", participantId)
    lastReadMessageKey = string.gsub(lastReadMessageKey, "{roomId}", roomId)

    local lastReadRaw = redis.call('GET', lastReadMessageKey)
    local lastReadId = tonumber(lastReadRaw)
    if lastReadId == nil then
        lastReadId = -1
    end

    -- 유저가 채팅방에 접속해있거나 메시지 보낸 유저면 자동 읽음 처리
    if isSender or isConnected == 1 then
        -- 혹시 이미 읽음 처리되어있다면 pass
        if lastReadId < messageId then
            -- 읽음 처리 (마지막 읽은 아이디 업뎃)
            redis.call('SET', lastReadMessageKey, messageId)
            table.insert(readUsers, participantId)

            -- 새 메시지 이전의 메시지들 읽음 처리(요청 순서대로 레디스에서 처리하지 않을 경우를 위해)
            local unreadMessages = redis.call('ZRANGEBYSCORE', messageZSetKey, lastReadId + 1, messageId - 1)
            for _, mid in ipairs(unreadMessages or {}) do
                redis.call('HINCRBY', unreadCountHashKey, mid, -1)
            end
        end
    else
        -- 안읽음 수 + 1
        unreadCount = unreadCount + 1
    end
end

-- 현재 메시지 unreadCount 저장
redis.call('HSET', unreadCountHashKey, messageId, unreadCount)

-- 결과 반환: {unreadCount, 읽은 유저 ID 리스트}
return { unreadCount, readUsers }
