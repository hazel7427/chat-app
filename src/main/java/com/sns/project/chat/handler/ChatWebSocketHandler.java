package com.sns.project.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.dto.KafkaChatMessageDto;
import com.sns.project.chat.dto.response.ChatMessageResponse;
import com.sns.project.chat.kafka.producer.MessageProducer;
import com.sns.project.chat.service.ChatPresenceService;
import com.sns.project.chat.service.ChatReadService;
import com.sns.project.chat.service.ChatService;
import com.sns.project.repository.chat.ChatMessageRepository;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatPresenceService chatPresenceService;
    private final ChatReadService chatReadService;
    private final MessageProducer messageProducer; // ✅ Kafka 프로듀서
    private final ChatService chatService;

    public ChatWebSocketHandler(
        @Qualifier("chatRedisTemplate") RedisTemplate<String, String> redisTemplate,
        ChatPresenceService chatPresenceService,
        ChatReadService chatReadService,
        MessageProducer messageProducer,
        ChatService chatService
    ) {
        this.redisTemplate = redisTemplate;
        this.chatPresenceService = chatPresenceService;
        this.chatReadService = chatReadService;
        this.messageProducer = messageProducer;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("✅ WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");

        if (userId != null && roomId != null) {
            chatPresenceService.userLeftRoom(roomId, userId);
            log.info("👋 User {} left room {}", userId, roomId);
            roomSessions.getOrDefault(roomId, new HashSet<>()).remove(session);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject json = new JSONObject(message.getPayload());
        String type = json.getString("type");

        if ("JOIN".equals(type)) {
            Long roomId = json.getLong("roomId");
            Long userId = (Long) session.getAttributes().get("userId");

            session.getAttributes().put("roomId", roomId);
            roomSessions.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
            roomSessions.get(roomId).add(session);

            JSONObject payload = new JSONObject();
            payload.put("type", "JOIN");
            payload.put("roomId", roomId);
            payload.put("senderId", userId);

            String streamKey = "chat-stream:" + roomId;
            redisTemplate.opsForStream().add(streamKey, Collections.singletonMap("join", payload.toString()));

            log.info("🙋‍♂️ User {} joined room {}", userId, roomId);

        } else if ("MESSAGE".equals(type)) {
            Long roomId = json.getLong("roomId");
            String msg = json.getString("message");
            Long senderId = (Long) session.getAttributes().get("userId");

            Long messageId = chatService.saveMessage(roomId, senderId, msg);
            KafkaChatMessageDto kafkaChatMessage = KafkaChatMessageDto.builder()
                .messageId(messageId)
                .roomId(roomId)
                .senderId(senderId)
                .content(msg)
                .timestamp(System.currentTimeMillis())
                .build();
            
            messageProducer.send(kafkaChatMessage);
            log.info("📤 Kafka로 메시지 전송됨: {}", kafkaChatMessage);
        }
    }

    public void broadcastToRoom(ChatMessageResponse message) throws IOException {
        Long roomId = message.getRoomId();
//        List<Long> readUserIds = message.getReadUserIds();
        
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return;
        String jsonMessage = objectMapper.writeValueAsString(message);

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(jsonMessage));
            }
        }
        log.info("📩 Message sent to room {}: {}", roomId, message);
    }
}
