// MessageUnreadProducer.java
package com.sns.project.chat.kafka.producer;

import com.sns.project.chat.kafka.dto.request.KafkaProcessUnreadRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageUnreadProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "message.unread";

    public void send(KafkaProcessUnreadRequest message) {
        kafkaTemplate.send(TOPIC, message.getRoomId().toString(), message);
    }
}
