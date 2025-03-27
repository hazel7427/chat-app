package com.sns.project.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.kafka.dto.request.KafkaNewMsgCacheRequest;
import com.sns.project.chat.dto.websocket.RoomScopedPayload;
import com.sns.project.chat.kafka.producer.MessageCacheProducer;
import com.sns.project.chat.service.ChatPresenceService;
import com.sns.project.chat.service.ChatReadService;
import com.sns.project.chat.service.ChatRedisService;
import com.sns.project.chat.service.ChatService;
import com.sns.project.config.constants.RedisKeys.Chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatPresenceService chatPresenceService;
    private final ChatReadService chatReadService;
    private final MessageCacheProducer messageProducer; // ✅ Kafka 프로듀서
    private final ChatService chatService;
    private final ChatRedisService chatRedisService;

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
        log.info("json: {}", json);
        if ("JOIN".equals(type)) {
            Long roomId = json.getLong("roomId");
            Long userId = (Long) session.getAttributes().get("userId");

            session.getAttributes().put("roomId", roomId);
            roomSessions.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
            roomSessions.get(roomId).add(session);

            log.info("🍉 user {} joined room {}", userId, roomId);
            String connectedUserKey = Chat.CONNECTED_USERS_SET_KEY.getConnectedKey(roomId);
            chatRedisService.addToSet(connectedUserKey, userId.toString());

//            JSONObject payload = new JSONObject();
//            payload.put("type", "JOIN");
//            payload.put("roomId", roomId);
//            payload.put("senderId", userId);
//
//            String streamKey = "chat-stream:" + roomId;
//            redisTemplate.opsForStream().add(streamKey, Collections.singletonMap("join", payload.toString()));
//
//            log.info("🙋‍♂️ User {} joined room {}", userId, roomId);

        } else if ("MESSAGE".equals(type)) {
            Long roomId = json.getLong("roomId");
            String msg = json.getString("message");
            Long senderId = (Long) session.getAttributes().get("userId");

            Long messageId = chatService.saveMessage(roomId, senderId, msg);
            KafkaNewMsgCacheRequest kafkaChatMessage = KafkaNewMsgCacheRequest.builder()
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

    public void broadcastToRoom(RoomScopedPayload payload) throws IOException {
        Long roomId = payload.getRoomId();
//        List<Long> readUserIds = message.getReadUserIds();
        log.info("🍉 broadcast to room : {}", roomId);
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        log.info("session : {}", sessions);
        if (sessions == null || sessions.isEmpty()) return;
        String jsonMessage = objectMapper.writeValueAsString(payload);

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(jsonMessage));
            }
        }
        log.info("📩 Message sent to room {}: {}", roomId, payload);
    }
}
