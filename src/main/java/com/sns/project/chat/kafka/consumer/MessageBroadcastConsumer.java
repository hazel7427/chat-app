package com.sns.project.chat.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.dto.response.ChatMessageResponse;
import com.sns.project.chat.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageBroadcastConsumer {

    private final ChatWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "message.broadcast", groupId = "chat-broadcast-group")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String payload = record.value();
            ChatMessageResponse response = objectMapper.readValue(payload, ChatMessageResponse.class);
            log.info("📡 Broadcast 수신: {}", response);

            webSocketHandler.broadcastToRoom(response);
        } catch (Exception e) {
            log.error("🚨 Broadcast 메시지 처리 실패", e);
            // TODO: DLQ 또는 에러 큐 연동 고려
        }
    }
}
