package com.sns.project.chat.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.sns.project.chat.kafka.dto.request.KafkaBroadcastRequest;
import com.sns.project.chat.kafka.dto.request.KafkaReadPersistRequest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MessageReadProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendBroadcast(KafkaBroadcastRequest event) {
        kafkaTemplate.send("message.broadcast", event.getRoomId().toString(), event);
    }

    public void sendReadPersist(KafkaReadPersistRequest event) {
        kafkaTemplate.send("message.read.persist", event.getRoomId().toString(), event);
    }
}
