package com.sns.project.chat.dto;

import java.util.List;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KafkaReadMessageDto {
    private Long roomId;
    private Long messageId;
    private String content;
    private Set<Long> readUserIds; // 이 메시지를 읽은 유저들
    private Long unreadCount;
    private Long senderId;
}
