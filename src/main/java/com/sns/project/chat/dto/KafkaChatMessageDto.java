// ChatMessage.java
package com.sns.project.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class KafkaChatMessageDto {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String content;
    private Long timestamp;
}
