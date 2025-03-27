package com.sns.project.chat.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.kafka.dto.request.KafkaNewMsgCacheRequest;
import com.sns.project.chat.kafka.dto.request.KafkaProcessUnreadRequest;
import com.sns.project.chat.kafka.producer.MessageUnreadProducer;
import com.sns.project.chat.service.ChatRedisService;
import com.sns.project.config.constants.RedisKeys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCacheConsumer {

    private final ChatRedisService chatRedisService;
    private final MessageUnreadProducer messageUnreadProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "message.cache", groupId = "chat-cache-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String json, Acknowledgment ack) throws JsonProcessingException {
        KafkaNewMsgCacheRequest message = objectMapper.readValue(json, KafkaNewMsgCacheRequest.class);
        Long roomId = message.getRoomId();
        Long messageId = message.getMessageId();

        log.info("📥 Kafka 수신 메시지 (캐시): roomId={}, messageId={}", roomId, messageId);

        // 1. 메시지를 Redis ZSet에 캐시
        String messageZSetKey = RedisKeys.Chat.CHAT_MESSAGES_KEY.getMessagesKey(roomId);
        chatRedisService.addToZSet(messageZSetKey, messageId.toString(), messageId);

        log.info("✅ Redis 메시지 캐시 완료: {}", messageZSetKey);

        // 2. unread 계산용 Kafka 전송
        KafkaProcessUnreadRequest request = new KafkaProcessUnreadRequest(message);
        messageUnreadProducer.send(request);
        log.info("📨 Kafka 전송 (unread 계산): {}", request);

        ack.acknowledge();
    }
}
