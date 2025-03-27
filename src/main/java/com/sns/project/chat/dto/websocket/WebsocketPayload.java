package com.sns.project.chat.dto.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sns.project.chat.dto.response.ReadBroadcast;
import com.sns.project.chat.dto.websocket.MessageBroadcast;
import com.sns.project.chat.dto.websocket.JoinBroadcast;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MessageBroadcast.class, name = "MESSAGE"),
    @JsonSubTypes.Type(value = JoinBroadcast.class, name = "JOIN"),
    @JsonSubTypes.Type(value = ReadBroadcast.class, name = "READ")
})
public interface WebsocketPayload {
    String getType();
}
