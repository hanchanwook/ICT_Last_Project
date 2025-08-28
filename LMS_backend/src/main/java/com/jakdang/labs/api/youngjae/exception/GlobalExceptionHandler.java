package com.jakdang.labs.api.youngjae.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.jakdang.labs.api.youngjae.dto.ErrorResponse;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(e.getMessage()));
    }
    
    /**
     * ChatRoomNotFoundException 처리
     */
    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomNotFoundException(ChatRoomNotFoundException e) {
        log.error("ChatRoomNotFoundException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(e.getMessage()));
    }
    
    /**
     * UserNotFoundException 처리
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        log.error("UserNotFoundException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(e.getMessage()));
    }
    
    /**
     * UnauthorizedException 처리
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
        log.error("UnauthorizedException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(e.getMessage()));
    }
    
    /**
     * MethodArgumentNotValidException 처리 (Validation 오류)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        
        log.error("Validation error: {}", errorMessage);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(errorMessage));
    }
    
    /**
     * 정적 리소스를 찾을 수 없을 때 처리
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("요청한 리소스를 찾을 수 없습니다."));
    }
    
    /**
     * 일반적인 Exception 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Internal server error occurred: " + e.getMessage()));
    }
} 