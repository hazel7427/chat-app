package com.sns.project.chat.kafka.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class KafkaNewMsgCacheRequest {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String content;
    private Long timestamp;
}
