package com.sns.project.chat.kafka.dto.request;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class KafkaReadPersistRequest {
    private Long messageId;
    private Long roomId;
    private Set<Long> readUserIds;
    
    public KafkaReadPersistRequest(Long messageId, Long roomId, Set<Long> readUserIds) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.readUserIds = readUserIds;
    }
}
