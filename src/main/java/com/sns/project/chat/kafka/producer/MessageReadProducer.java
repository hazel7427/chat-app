package com.sns.project.chat.kafka.producer;

import com.sns.project.chat.dto.response.ChatMessageResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.sns.project.chat.dto.KafkaReadMessageDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MessageReadProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendBroadcast(KafkaReadMessageDto event) {
        Long roomId = event.getRoomId();
        Long senderId = event.getSenderId();
        String content = event.getContent();

        ChatMessageResponse response = ChatMessageResponse.builder()
                        .roomId(roomId)
                        .messageId(event.getMessageId())
                        .content(content)
                        .senderId(senderId)
                        .build();
        kafkaTemplate.send("message.broadcast", event.getRoomId().toString(), response);
    }

    public void sendReadPersist(KafkaReadMessageDto event) {
        kafkaTemplate.send("message.read.persist", event.getRoomId().toString(), event);
    }
}
