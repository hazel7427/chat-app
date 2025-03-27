package com.sns.project.chat.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.kafka.dto.request.KafkaReadPersistRequest;
import com.sns.project.chat.service.ChatReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageReadPersistConsumer {

    private final ChatReadService chatReadService;
    private final ObjectMapper objectMapper;    

    @KafkaListener(topics = "message.read.persist", groupId = "chat-read-persist-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String json, Acknowledgment ack) throws JsonProcessingException {
        KafkaReadPersistRequest event = objectMapper.readValue(json, KafkaReadPersistRequest.class);
        log.info("📝 Kafka 수신 메시지 (읽음 디비 저장): {}", event);

        for (Long userId : event.getReadUserIds()) {
            chatReadService.saveOrUpdateReadStatus(userId, event.getRoomId(), event.getMessageId());
        }
        ack.acknowledge();
    }
}

