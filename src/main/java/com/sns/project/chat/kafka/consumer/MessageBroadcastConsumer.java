package com.sns.project.chat.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.dto.websocket.MessageBroadcast;
import com.sns.project.chat.dto.websocket.RoomScopedPayload;
import com.sns.project.chat.handler.ChatWebSocketHandler;
import com.sns.project.chat.kafka.dto.request.KafkaBroadcastRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageBroadcastConsumer {

    private final ChatWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "message.broadcast", groupId = "chat-broadcast-group", 
    containerFactory = "kafkaListenerContainerFactory" // 커스텀 팩토리 지정
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String payload = record.value();
            KafkaBroadcastRequest response = objectMapper.readValue(payload, KafkaBroadcastRequest.class);
            log.info("📡 Broadcast 수신: {}", response);

            // 웹소켓 브로드캐스트
            RoomScopedPayload broadcastPayload = MessageBroadcast.builder()
                .roomId(response.getRoomId())
                .senderId(response.getSenderId())
                .content(response.getContent())
                .timestamp(response.getTimestamp())
                .unreadCount(response.getUnreadCount())
                .build();
                
            webSocketHandler.broadcastToRoom(broadcastPayload);
            ack.acknowledge(); 
        } catch (Exception e) {
            log.error("🚨 Broadcast 메시지 처리 실패", e);
            // TODO: DLQ 또는 에러 큐 연동 고려
        }
    }
}
