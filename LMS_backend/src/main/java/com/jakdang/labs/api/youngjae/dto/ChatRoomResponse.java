package com.jakdang.labs.api.youngjae.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import com.jakdang.labs.entity.ChatMessageEntity;
import com.jakdang.labs.entity.ChatRoomEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private String id;
    private String name;
    private String lastMessage;
    private Instant lastMessageTimestamp; // 마지막 메시지 작성시간 (ISO 형식)
    private Integer unread;
    
    public static ChatRoomResponse from(ChatRoomEntity chatRoom, ChatMessageEntity lastMessage) {
        String lastMessageContent = "";
        Instant lastMessageTimestamp = null;
        
        if (lastMessage != null && lastMessage.getCreatedAt() != null) {
            lastMessageContent = lastMessage.getContent();
            lastMessageTimestamp = lastMessage.getCreatedAt();
        } else if (chatRoom.getCreatedAt() != null) {
            lastMessageTimestamp = chatRoom.getCreatedAt();
        }

        
        return ChatRoomResponse.builder()
                .id(chatRoom.getChatRoomId())
                .name(chatRoom.getChatRoomName())
                .lastMessage(lastMessageContent)
                .lastMessageTimestamp(lastMessageTimestamp)
                .unread(null) // 읽지 않은 메시지 수는 서비스에서 계산
                .build();
    }
} 