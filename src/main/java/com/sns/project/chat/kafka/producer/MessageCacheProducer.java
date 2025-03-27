// MessageProducer.java
package com.sns.project.chat.kafka.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.sns.project.chat.kafka.dto.request.KafkaNewMsgCacheRequest;


@Service
@RequiredArgsConstructor
public class MessageCacheProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "message.cache";

    public void send(KafkaNewMsgCacheRequest message) {
        kafkaTemplate.send(TOPIC, message.getRoomId().toString(), message);
    }
}
