package com.sns.project.chat.service;


import com.sns.project.config.constants.RedisKeys.Chat;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sns.project.config.constants.RedisKeys;
import com.sns.project.domain.chat.ChatMessage;
import com.sns.project.domain.chat.ChatRoom;
import com.sns.project.domain.user.User;
import com.sns.project.handler.exceptionHandler.exception.notfound.ChatRoomNotFoundException;
import com.sns.project.chat.dto.response.ChatMessageResponse;
import com.sns.project.repository.chat.ChatMessageRepository;
import com.sns.project.repository.chat.ChatRoomRepository;
import com.sns.project.repository.chat.ChatParticipantRepository;
import com.sns.project.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserService userService;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatReadService chatReadService;
    private final RedisLuaService redisLuaService;

    private final ChatRedisService stringRedisService;


    @Transactional
    public ChatMessageResponse saveMessage(Long senderId, String message, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
        User sender = userService.getUserById(senderId);

        // 1. Save message and cache in Redis
        ChatMessage savedMessage = saveAndCacheMessage(chatRoom, sender, message, roomId);


        // 2. Process unread count with Lua script
        MessageProcessResult result = redisLuaService.processNewMessage(
            Chat.CHAT_ROOM_PARTICIPANTS_SET_KEY.getParticipants(roomId),
            Chat.CONNECTED_USERS_SET_KEY.getConnectedKey(roomId),
            Chat.CHAT_UNREAD_COUNT_HASH_KEY.getUnreadCountKey(),
            Chat.CHAT_LAST_READ_MESSAGE_ID.getLastReadMessageKeyPattern(),
            Chat.CHAT_MESSAGES_KEY.getMessagesKey(roomId),
            roomId.toString(),
            savedMessage.getId().toString(),
            senderId.toString()
        );

//        System.out.println("OMGOMG" + unreadCount);
//        System.out.println(stringRedisService.getSetMembers(Chat.CHAT_ROOM_PARTICIPANTS_SET_KEY.getParticipants(roomId)));
//        System.out.println(stringRedisService.getSetMembers(Chat.CONNECTED_USERS_SET_KEY.getConnectedKey(roomId)));

        System.out.println("OMGOMG");
        result.getReadUsers().forEach(System.out::println);
        return new ChatMessageResponse(savedMessage, result.getUnreadCount());
    }

    private ChatMessage saveAndCacheMessage(ChatRoom chatRoom, User sender, String message, Long roomId) {
        ChatMessage chatMessage = new ChatMessage(chatRoom, sender, message);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        String messageKey = RedisKeys.Chat.CHAT_MESSAGES_KEY.getMessagesKey(roomId);
        stringRedisService.addToZSet(messageKey, savedMessage.getId().toString(),
        savedMessage.getId());

        log.info("Chat message saved successfully. MessageId: {}, RoomId: {}", savedMessage.getId(), roomId);
        return savedMessage;
    }




    @Transactional
    public List<ChatMessageResponse> getChatHistory(Long roomId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdWithUser(roomId);
        return messages.stream().map(msg -> {
            String unreadKey = RedisKeys.Chat.CHAT_UNREAD_COUNT_HASH_KEY.getUnreadCountKey();
            String unreadCount = stringRedisService.getHashValue(unreadKey, String.valueOf(msg.getId()))
            .orElseThrow(() -> new RuntimeException("Unread count not found"));
            return new ChatMessageResponse(msg, Long.valueOf(unreadCount));
        }).collect(Collectors.toList());
    }
}
