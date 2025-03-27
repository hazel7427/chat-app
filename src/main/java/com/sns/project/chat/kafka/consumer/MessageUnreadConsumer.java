package com.sns.project.chat.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.kafka.dto.request.KafkaBroadcastRequest;
import com.sns.project.chat.kafka.dto.request.KafkaReadPersistRequest;
import com.sns.project.chat.kafka.dto.request.KafkaProcessUnreadRequest;
import com.sns.project.chat.service.UnreadCountService;
import com.sns.project.chat.service.dto.UnreadCountAndReadUsers;
import com.sns.project.chat.kafka.producer.MessageReadProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageUnreadConsumer {

    private final UnreadCountService unreadCountService;
    private final MessageReadProducer readProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "message.unread", groupId = "chat-unread-group", containerFactory = "kafkaListenerContainerFactory")
    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void consume(String json, Acknowledgment ack) throws JsonProcessingException {
        KafkaProcessUnreadRequest message = objectMapper.readValue(json, KafkaProcessUnreadRequest.class);
        log.info("🧭 Kafka 수신 메시지 (unread 계산): {}", message);

        try {
            // 1. 비즈니스 처리 (Redis Lua 실행)
            UnreadCountAndReadUsers result = unreadCountService.handleUnreadCalculation(
                message.getRoomId(),
                message.getMessageId(),
                message.getSenderId()
            );

            // 2. 브로드캐스트용 DTO 생성
            KafkaBroadcastRequest broadcastDto = new KafkaBroadcastRequest(message, result.getUnreadCount());
            // 3. DB 저장용 DTO 생성
            KafkaReadPersistRequest persistDto = new KafkaReadPersistRequest(
                message.getMessageId(),
                message.getRoomId(),
                result.getReadUsers()
            );

            // 4. Kafka 전송
            readProducer.sendBroadcast(broadcastDto);
            readProducer.sendReadPersist(persistDto);

        } catch (Exception e) {
            log.error("🚨 unread 계산 실패: {}", message, e);
        }

        ack.acknowledge();
    }
}
