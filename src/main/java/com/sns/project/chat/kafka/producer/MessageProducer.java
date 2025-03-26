// MessageProducer.java
package com.sns.project.chat.kafka.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.sns.project.chat.dto.KafkaChatMessageDto;

@Service
@RequiredArgsConstructor
public class MessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "message.cache";

    public void send(KafkaChatMessageDto message) {
        kafkaTemplate.send(TOPIC, message.getRoomId().toString(), message);
    }
}
