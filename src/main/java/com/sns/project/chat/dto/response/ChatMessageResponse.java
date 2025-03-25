package com.sns.project.chat.dto.response;

import java.time.format.DateTimeFormatter;

import com.sns.project.domain.chat.ChatMessage;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ChatMessageResponse {
    private Long id;
    private String message;
    private String sentAt;
    private Long senderId;
    private String senderName;
    private Long unreadCount;

    public ChatMessageResponse(ChatMessage chatMessage, Long unreadCount) {
        this.id = chatMessage.getId();
        this.message = chatMessage.getMessage();
        this.sentAt = chatMessage.getSentAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.senderId = chatMessage.getSender().getId();
        this.senderName = chatMessage.getSender().getName();
        this.unreadCount = unreadCount;
    }
}
