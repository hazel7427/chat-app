package com.sns.project.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {

    private String type;            // "MESSAGE", "READ", "JOIN" 등
    private Long messageId;         // 메시지 ID (MESSAGE, READ 타입일 때)
    private Long roomId;            // 채팅방 ID
    private Long senderId;          // 보낸 유저 ID
    private String content;         // 메시지 내용 (MESSAGE 타입일 때)
    private Long timestamp;         // 메시지 전송 시간 (선택)

    private Integer unreadCount;    // 안 읽은 수 (MESSAGE 타입일 때)
//    private List<Long> readUserIds; // 읽은 유저 ID 목록 (READ 타입일 때)
}
