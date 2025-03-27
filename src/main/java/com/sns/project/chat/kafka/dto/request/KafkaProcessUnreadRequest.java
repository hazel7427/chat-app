package com.sns.project.chat.kafka.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KafkaProcessUnreadRequest {
    private Long roomId;
    private Long messageId;
    private Long senderId;
    private String content;
    private Long timestamp;

    public KafkaProcessUnreadRequest(KafkaNewMsgCacheRequest message) {
        this.roomId = message.getRoomId();
        this.messageId = message.getMessageId();
        this.senderId = message.getSenderId();
        this.content = message.getContent();
        this.timestamp = message.getTimestamp();
    }
}
