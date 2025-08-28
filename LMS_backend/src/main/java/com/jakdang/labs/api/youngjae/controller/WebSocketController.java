package com.jakdang.labs.api.youngjae.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.jakdang.labs.api.youngjae.dto.SendMessageRequest;
import com.jakdang.labs.api.youngjae.dto.SendMessageResponse;
import com.jakdang.labs.api.youngjae.dto.TypingMessage;
import com.jakdang.labs.api.youngjae.dto.UserStatusMessage;
import com.jakdang.labs.api.youngjae.dto.WebSocketMessage;
import com.jakdang.labs.api.youngjae.service.ChatRoomService;
import com.jakdang.labs.security.jwt.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket API", description = "실시간 채팅 WebSocket API")
public class WebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final JwtUtil jwtUtil;
    
    /**
     * 사용중
     * WebSocket 메시지 전송
     * POST /app/chat.sendMessage
     */
    @Operation(summary = "WebSocket 메시지 전송", description = "실시간 채팅 메시지를 전송합니다")
    @MessageMapping("/chat.sendMessage")
    public WebSocketMessage sendMessage(@Payload SendMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 세션 정보 안전하게 추출
            String roomId = getSessionAttribute(headerAccessor, "roomId");
            String userId = getSessionAttribute(headerAccessor, "userId");
            String userName = getSessionAttribute(headerAccessor, "userName");
            
            if (roomId == null || userId == null) {
                            // log.error("세션 정보 누락: roomId={}, userId={}", roomId, userId);
            // log.error("사용자 정보를 복원할 수 없습니다.");
                return null;
            }
            
                        // log.info("WebSocket 메시지 전송: roomId={}, userId={}, userName={}, content={}",
            //         roomId, userId, userName, request.getContent());
            
                                 // 메시지 저장 (userEmail 사용)
                     String userEmail = getSessionAttribute(headerAccessor, "userEmail");
                     if (userEmail == null) {
                         // log.error("userEmail이 없습니다.");
                         return null;
                     }
                     SendMessageResponse.MessageDto savedMessage = chatRoomService.sendMessage(roomId, userEmail, request);
            
            // WebSocket 메시지 생성
            WebSocketMessage webSocketMessage = WebSocketMessage.builder()
                    .id(savedMessage.getId())
                    .message(WebSocketMessage.MessageDto.from(savedMessage))
                    .type("message")
                    .roomId(roomId)
                    .build();
            

            
            // 채팅방의 모든 구독자에게 메시지 전송
            // log.info("브로드캐스트 전송 시작: /topic/room/{}", roomId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, webSocketMessage);
            // log.info("브로드캐스트 전송 완료: {}", webSocketMessage);
            return webSocketMessage;
            
        } catch (Exception e) {
            // log.error("WebSocket 메시지 전송 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * 사용중
     * WebSocket 사용자 추가
     * POST /app/chat.addUser
     */
    @Operation(summary = "WebSocket 사용자 추가", description = "WebSocket 채팅방에 사용자를 추가합니다")
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload String roomId, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 세션 정보 안전하게 추출
            String userId = getSessionAttribute(headerAccessor, "userId");
            String userName = getSessionAttribute(headerAccessor, "userName");
            
            // 세션 정보가 없으면 토큰에서 복원 시도
            if (userId == null || userName == null) {
                // log.warn("세션 정보가 누락되었습니다. 토큰에서 복원을 시도합니다.");
                restoreSessionFromToken(headerAccessor);
                
                // 다시 세션 정보 추출
                userId = getSessionAttribute(headerAccessor, "userId");
                userName = getSessionAttribute(headerAccessor, "userName");
                
                            if (userId == null || userName == null) {
                // log.error("토큰에서도 사용자 정보를 복원할 수 없습니다: userId={}, userName={}", userId, userName);
                // log.error("사용자 정보를 복원할 수 없습니다.");
                return;
            }
            }
            
            // log.info("WebSocket 사용자 추가: roomId={}, userId={}, userName={}", roomId, userId, userName);
            
            // 세션에 채팅방 정보 저장
            headerAccessor.getSessionAttributes().put("roomId", roomId);
            // log.info("세션에 roomId 저장 완료: {}", roomId);
            

            
            // 채팅방의 모든 구독자에게 사용자 입장 알림 전송
            UserStatusMessage joinMessage = UserStatusMessage.builder()
                    .type("user_joined")
                    .roomId(roomId)
                    .userId(userId)
                    .userName(userName)
                    .build();
            
            // log.info("사용자 입장 알림 전송: /topic/room/{}/status", roomId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/status", joinMessage);
            // log.info("사용자 입장 알림 전송 완료");
            
        } catch (Exception e) {
            // log.error("WebSocket 사용자 추가 중 오류 발생", e);
        }
    }
    
    /**
     * 사용중
     * WebSocket 타이핑 상태 처리
     * POST /app/chat.typing
     */
    @Operation(summary = "WebSocket 타이핑 상태", description = "사용자의 타이핑 상태를 처리합니다")
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingMessage typingMessage, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 세션 정보 안전하게 추출
            String roomId = getSessionAttribute(headerAccessor, "roomId");
            String userId = getSessionAttribute(headerAccessor, "userId");
            String userName = getSessionAttribute(headerAccessor, "userName");
            
            if (roomId == null || userId == null || userName == null) {
                // log.error("세션 정보 누락: roomId={}, userId={}, userName={}", roomId, userId, userName);
                return;
            }
            
                        // log.info("WebSocket 타이핑 상태: roomId={}, userId={}, userName={}, isTyping={}",
            //         roomId, userId, userName, typingMessage.isTyping());
            

            
            // 타이핑 상태 메시지 생성
            TypingMessage typingStatus = TypingMessage.builder()
                    .type("typing")
                    .roomId(roomId)
                    .userId(userId)
                    .userName(userName)
                    .isTyping(typingMessage.isTyping())
                    .build();
            
            // 채팅방의 다른 사용자들에게 타이핑 상태 전송 (발신자 제외)
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/typing", typingStatus);
            
        } catch (Exception e) {
            // log.error("WebSocket 타이핑 상태 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 세션 속성을 안전하게 추출하는 헬퍼 메서드
     */
    private String getSessionAttribute(SimpMessageHeaderAccessor headerAccessor, String key) {
        Object value = headerAccessor.getSessionAttributes().get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 토큰에서 세션 정보를 복원하는 메서드
     */
    private void restoreSessionFromToken(SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 쿠키에서 토큰 추출
            List<String> cookies = headerAccessor.getNativeHeader("Cookie");
            // log.info("쿠키 정보: {}", cookies);
            
            String token = null;
            if (cookies != null && !cookies.isEmpty()) {
                String cookieHeader = cookies.get(0);
                String[] cookiePairs = cookieHeader.split(";");
                for (String pair : cookiePairs) {
                    String[] keyValue = pair.trim().split("=");
                    if (keyValue.length == 2 && "refresh".equals(keyValue[0])) {
                        token = keyValue[1];
                        break;
                    }
                }
            }
            
            // log.info("추출된 토큰: {}", token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "null");
            
            if (token != null && !jwtUtil.isExpired(token)) {
                String userEmail = jwtUtil.getUserEmail(token);
                String userId = jwtUtil.getUserId(token);
                String userName = jwtUtil.getUsername(token);
                
                // log.info("토큰에서 사용자 정보 복원: email={}, userId={}, userName={}", userEmail, userId, userName);

                // 세션에 사용자 정보 저장
                if (headerAccessor.getSessionAttributes() == null) {
                    headerAccessor.setSessionAttributes(new HashMap<>());
                }
                headerAccessor.getSessionAttributes().put("userId", userId);
                headerAccessor.getSessionAttributes().put("userName", userName);
                headerAccessor.getSessionAttributes().put("userEmail", userEmail);
                
                // log.info("세션에 사용자 정보 복원 완료: {}", headerAccessor.getSessionAttributes());
            } else {
                                // log.error("토큰이 유효하지 않거나 추출할 수 없습니다. token={}, expired={}", 
                //         token != null, token != null ? jwtUtil.isExpired(token) : "N/A");
            }
        } catch (Exception e) {
            // log.error("토큰에서 세션 정보 복원 중 오류 발생", e);
        }
    }
} 