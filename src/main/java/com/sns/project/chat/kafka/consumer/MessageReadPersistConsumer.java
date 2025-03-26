package com.sns.project.chat.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.project.chat.dto.KafkaReadMessageDto;
import com.sns.project.chat.service.ChatReadService;
import com.sns.project.repository.chat.ChatReadStatusRepository;
import com.sns.project.domain.chat.ChatReadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageReadPersistConsumer {

    private final ChatReadService chatReadService;
    private final ObjectMapper objectMapper;    

    @KafkaListener(topics = "message.read.persist", groupId = "chat-read-persist-group")
    public void consume(String json) throws JsonProcessingException {
        KafkaReadMessageDto event = objectMapper.readValue(json, KafkaReadMessageDto.class);
        log.info("📝 Kafka 수신 메시지 (읽음 디비 저장): {}", event);

        for (Long userId : event.getReadUserIds()) {
            chatReadService.saveOrUpdateReadStatus(userId, event.getRoomId(), event.getMessageId());
        }
    }
}

