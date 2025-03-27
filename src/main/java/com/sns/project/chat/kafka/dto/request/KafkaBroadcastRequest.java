package com.sns.project.chat.kafka.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KafkaBroadcastRequest {
    public KafkaBroadcastRequest(KafkaProcessUnreadRequest message, Long unreadCount) {
        this.messageId = message.getMessageId();
        this.roomId = message.getRoomId();
        this.senderId = message.getSenderId();
        this.content = message.getContent();
        this.timestamp = message.getTimestamp();
        this.unreadCount = unreadCount;
    }
    private String type = "MESSAGE";
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String content;
    private Long timestamp;
    private Long unreadCount;
}
