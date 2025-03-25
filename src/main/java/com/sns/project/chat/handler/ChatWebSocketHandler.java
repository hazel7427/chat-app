
package com.sns.project.chat.handler;

import com.sns.project.chat.service.ChatReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.dto.response.ChatMessageResponse;
import com.sns.project.chat.service.ChatPresenceService;

import org.json.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ ObjectMapper 추가
    private final ChatPresenceService chatPresenceService;
    private final ChatReadService chatReadService;

    public ChatWebSocketHandler(@Qualifier("chatRedisTemplate") RedisTemplate<String, String> redisTemplate, ChatPresenceService chatPresenceService,
        ChatReadService chatReadService) {
        this.redisTemplate = redisTemplate;
        this.chatPresenceService = chatPresenceService;
        this.chatReadService = chatReadService;
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
            log.info("User {} left room {}", userId, roomId);
        System.out.println(session);
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


        } else if ("MESSAGE".equals(type)) {
            int roomId = json.getInt("roomId");
            String msg = json.getString("message");
            Long senderId = (Long) session.getAttributes().get("userId");

            JSONObject payload = new JSONObject();
            payload.put("type", "MESSAGE");
            payload.put("roomId", roomId);
            payload.put("senderId", senderId);
            payload.put("message", msg);

            String streamKey = "chat-event";
            redisTemplate.opsForStream().add(streamKey, Collections.singletonMap("message", payload.toString()));
            System.out.println(payload);
            log.info("📤 Message saved "
                + ""
                + "to Redis Stream: {}", payload);
            log.info("🍇 user id: {}, room id: {}", senderId, roomId);
        }
    }

    public void broadcastToRoom(Long roomId, ChatMessageResponse message) throws IOException {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return;
        String jsonMessage = objectMapper.writeValueAsString(message); // ✅ DTO → JSON 문자열

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(jsonMessage));
            }
        }
        log.info("📩 Message sent to room {}: {}", roomId, message);
    }
}
