package com.sns.project.chat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import lombok.NoArgsConstructor;
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageBroadcast implements RoomScopedPayload {
    private final String type = "MESSAGE";
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String content;
    private Long timestamp;
    private Long unreadCount;

    @Override
    public Long getRoomId() {
        return roomId;
    }
}
