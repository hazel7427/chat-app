package com.sns.project.chat.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.dto.KafkaChatMessageDto;
import com.sns.project.chat.kafka.producer.MessageUnreadProducer;
import com.sns.project.chat.service.ChatRedisService;
import com.sns.project.config.constants.RedisKeys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCacheConsumer {

    private final ChatRedisService chatRedisService;
    private final MessageUnreadProducer messageUnreadProducer;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "message.cache", groupId = "chat-group")
    public void consume(String json) throws JsonProcessingException {
        KafkaChatMessageDto message = objectMapper.readValue(json, KafkaChatMessageDto.class);

        log.info("✅ Kafka 수신 메시지: {}", message);

        Long roomId = message.getRoomId();
        Long messageId = message.getMessageId();

        // 1. 메시지 ZSet에 추가
        String zsetKey = RedisKeys.Chat.CHAT_MESSAGES_KEY.getMessagesKey(roomId);
        chatRedisService.addToZSet(zsetKey, messageId.toString(), messageId);

        // 2. 메시지 안읽음 생성
        messageUnreadProducer.send(message);
        log.info("🧠 메시지 안읽음 생성 완료 - {}", message);

    }
}
