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

@Deprecated
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
    public Long saveMessage(Long roomId, Long senderId, String message) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
        User sender = userService.getUserById(senderId);

        // 1. Save message and cache in Redis
        ChatMessage savedMessage = chatMessageRepository.save(new ChatMessage(chatRoom, sender, message));
        
        return savedMessage.getId();
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





}
