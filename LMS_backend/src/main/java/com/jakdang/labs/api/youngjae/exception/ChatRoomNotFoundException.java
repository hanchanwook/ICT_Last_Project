package com.jakdang.labs.api.youngjae.exception;

public class ChatRoomNotFoundException extends RuntimeException {
    
    public ChatRoomNotFoundException(String message) {
        super(message);
    }
    
    public static ChatRoomNotFoundException of(String roomId) {
        return new ChatRoomNotFoundException("채팅방을 찾을 수 없습니다: " + roomId);
    }
} 