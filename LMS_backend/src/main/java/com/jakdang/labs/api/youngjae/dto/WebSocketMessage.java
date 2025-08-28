package com.jakdang.labs.api.youngjae.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import com.jakdang.labs.entity.ChatMessageEntity;
import com.jakdang.labs.entity.MemberEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String id;
    private String type;
    private String roomId;
    private MessageDto message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDto {
        private String id;
        private String content;
        private SenderDto sender;
        private Instant time;
        private String type;
        
        public static MessageDto from(SendMessageResponse.MessageDto messageDto) {
            return MessageDto.builder()
                    .id(messageDto.getId())
                    .content(messageDto.getContent())
                    .sender(SenderDto.from(messageDto.getSender()))
                    .time(messageDto.getTime())
                    .type(messageDto.getType())
                    .build();
        }
        
        public static MessageDto from(ChatMessageEntity message, MemberEntity sender) {
            return MessageDto.builder()
                    .id(message.getMessageId())
                    .content(message.getContent())
                    .sender(SenderDto.from(sender))
                    .time(message.getCreatedAt())
                    .type("message")
                    .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderDto {
        private String id;
        private String name;
        
        public static SenderDto from(SendMessageResponse.SenderDto senderDto) {
            return SenderDto.builder()
                    .id(senderDto.getId())
                    .name(senderDto.getName())
                    .build();
        }
        
        public static SenderDto from(MemberEntity user) {
            return SenderDto.builder()
                    .id(user.getId())
                    .name(user.getMemberName())
                    .build();
        }
    }
} 
