package com.jakdang.labs.api.youngjae.exception;

public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public static UnauthorizedException of(String action) {
        return new UnauthorizedException("권한이 없습니다: " + action);
    }
} 
