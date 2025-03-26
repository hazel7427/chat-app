package com.sns.project.chat.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class KafkaNewMessageDto {
    private final Long unreadCount;
    private final Set<Long> readUsers;
}