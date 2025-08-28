package com.jakdang.labs.api.youngjae.exception;

public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public static UserNotFoundException of(String userId) {
        return new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId);
    }
} 
