package com.sns.project.chat.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.dto.KafkaChatMessageDto;
import com.sns.project.chat.dto.KafkaNewMessageDto;
import com.sns.project.chat.dto.KafkaReadMessageDto;
import com.sns.project.chat.service.UnreadCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sns.project.chat.kafka.producer.MessageReadProducer;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageUnreadConsumer {

    private final UnreadCountService unreadCountService;
    private final MessageReadProducer readProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "message.unread", groupId = "chat-unread-group")
    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void consume(String json) throws JsonProcessingException {
        KafkaChatMessageDto message = objectMapper.readValue(json, KafkaChatMessageDto.class);
        log.info("🧭 Kafka 수신 메시지 (unread 계산): {}", message);
        try {
            KafkaNewMessageDto result = unreadCountService.handleUnreadCalculation(message);

            // DTO 만들기
            KafkaReadMessageDto dto = KafkaReadMessageDto.builder()
                .roomId(message.getRoomId())
                .messageId(message.getMessageId())
                .readUserIds(result.getReadUsers())
                .unreadCount(result.getUnreadCount())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .build();

            // 🔁 브로드캐스트 & 💾 디비저장용 이벤트 전송
            readProducer.sendBroadcast(dto);
            readProducer.sendReadPersist(dto);

        } catch (Exception e) {
            log.error("🚨 unread 계산 실패: {}", message, e);
        }
    }
}
