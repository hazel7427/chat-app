package com.sns.project.chat.service;

import com.sns.project.chat.dto.KafkaChatMessageDto;
import com.sns.project.chat.dto.KafkaNewMessageDto;
import com.sns.project.config.constants.RedisKeys.Chat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UnreadCountService {

    private final RedisLuaService redisLuaService;
    private final RedisPreloadService redisPreloadService;

    public KafkaNewMessageDto handleUnreadCalculation(KafkaChatMessageDto message) {
        Long roomId = message.getRoomId();
        Long messageId = message.getMessageId();
        Long senderId = message.getSenderId();

        // ✅ 캐시가 없을 경우 DB → Redis로 미리 적재
        redisPreloadService.preloadRoomData(roomId);
        log.info("✅ 캐시 미리 적재 완료: roomId={}", roomId);

        // ✅ Lua 스크립트로 unreadCount 계산 및 자동 읽음 처리
        KafkaNewMessageDto result = redisLuaService.processNewMessage(
            Chat.CHAT_ROOM_PARTICIPANTS_SET_KEY.getParticipants(roomId),
            Chat.CONNECTED_USERS_SET_KEY.getConnectedKey(roomId),
            Chat.CHAT_UNREAD_COUNT_HASH_KEY.getUnreadCountKey(),
            Chat.CHAT_LAST_READ_MESSAGE_ID.getLastReadMessageKeyPattern(),
            Chat.CHAT_MESSAGES_KEY.getMessagesKey(roomId),
            roomId.toString(),
            messageId.toString(),
            senderId.toString()
        );

        // 📦 결과 핸들링 (옵션)
        log.info("📊 unread 처리 완료: messageId={}, unreadCount={}, 읽은 유저={}",
            messageId, result.getUnreadCount(), result.getReadUsers());

        return result;
    }
}
