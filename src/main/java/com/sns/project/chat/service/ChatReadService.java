package com.sns.project.chat.service;

import com.sns.project.repository.UserRepository;
import com.sns.project.repository.chat.ChatMessageRepository;
import com.sns.project.repository.chat.ChatReadStatusRepository;
import com.sns.project.repository.chat.ChatRoomRepository;

import jakarta.transaction.Transactional;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import com.sns.project.config.constants.RedisKeys;
import com.sns.project.domain.chat.ChatReadStatus;
import com.sns.project.domain.chat.ChatRoom;
import com.sns.project.domain.user.User;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatReadService {
    private final ChatRedisService stringRedisService;

    private final RedisLuaService redisLuaService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadStatusRepository chatReadStatusRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public void saveOrUpdateReadStatus(Long userId, Long roomId, Long messageId) {
        User user = userRepository.getReferenceById(userId);
        ChatRoom room = chatRoomRepository.getReferenceById(roomId);        
        
        chatReadStatusRepository.findByUserIdAndRoomId(userId, roomId)
            .ifPresentOrElse(
                (status) -> {
                  System.out.println("원애릿음");
                  System.out.println(status);
                    chatReadStatusRepository.updateIfLastReadIdIsSmaller(userId, roomId, messageId);
                },
                () -> {
                  System.out.println("새로저장");
                    ChatReadStatus newStatus = new ChatReadStatus(user, room, messageId);
                    chatReadStatusRepository.save(newStatus);
                }
            );
    
    }
    
    /*
     * 유저의 읽지 않은 메시지를 조회하고 읽음 처리합니다.
     */
     public void markAllAsRead(Long userId, Long roomId) {
        
        String lastReadKey = RedisKeys.Chat.CHAT_LAST_READ_MESSAGE_ID.getLastReadMessageKey(userId, roomId);
        String messageZSetKey = RedisKeys.Chat.CHAT_MESSAGES_KEY.getMessagesKey(roomId);
        String unreadCountKey = RedisKeys.Chat.CHAT_UNREAD_COUNT_HASH_KEY.getUnreadCountKey();

        long curLastReadId = Long.parseLong(stringRedisService.getValue(lastReadKey).orElse("-1"));
        Optional<Long> newLastReadId = redisLuaService.processUnreadMessages(
            lastReadKey,
            messageZSetKey,
            unreadCountKey
        );
        // 읽음 처리할 메시지 없으면 종료
        if(newLastReadId.isEmpty()) {
            return;
        }

        for(long i = curLastReadId + 1; i <= newLastReadId.get(); i++) {
            String messageId = String.valueOf(i);
            Optional<String> count = stringRedisService.getHashValue(unreadCountKey, messageId);
            if(count.isPresent()) {
                notifyUnreadCount(Long.parseLong(messageId), Long.parseLong(count.get()));
            }
        }

    }



    private void notifyUnreadCount(Long messageId, Long count) {
        // messagingTemplate.convertAndSend("/topic/unread/" + messageId, count);
        // String message = "Unread count updated: " + count;
        // webSocketHandler.broadcastToAll(message); // ✅ WebSocket broadcast
        // log.info("Message {} unread count updated to {}", messageId, count);
    }


}

