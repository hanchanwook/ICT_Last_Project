package com.jakdang.labs.websocket;

import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {

    private final TokenUtils jwtTokenProvider;
    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            log.info("WebSocket {} 명령 처리: sessionId={}, sessionAttributes={}", 
                    accessor.getCommand(), accessor.getSessionId(), accessor.getSessionAttributes());
            
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // CONNECT 명령 처리
                log.info("=== CONNECT 명령 처리 시작 ===");
                handleConnect(accessor);
                log.info("=== CONNECT 명령 처리 완료 ===");
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                // SEND 명령에서 세션 정보 확인 및 복원
                log.info("=== SEND 명령 처리 시작 ===");
                handleSend(accessor);
                log.info("=== SEND 명령 처리 완료 ===");
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                // SUBSCRIBE 명령에서도 세션 정보 확인
                log.info("=== SUBSCRIBE 명령 처리 시작 ===");
                handleSend(accessor); // SEND와 동일한 로직 사용
                log.info("=== SUBSCRIBE 명령 처리 완료 ===");
            }
        }

        return message;
    }
    
    private void handleConnect(StompHeaderAccessor accessor) {
        // JWTFilter와 동일한 방식으로 토큰 추출
        String token = extractToken(accessor);
        
        log.info("토큰 검증 시작: token={}", token != null ? "있음" : "없음");
        
        if (token != null) {
            // 토큰 내용 디버깅
            try {
                String tokenCategory = jwtUtil.getCategory(token);
                String userEmail = jwtUtil.getUserEmail(token);
                boolean isExpired = jwtUtil.isExpired(token);
                
                log.info("토큰 정보: category={}, email={}, expired={}", tokenCategory, userEmail, isExpired);
            } catch (Exception e) {
                log.error("토큰 파싱 중 오류: {}", e.getMessage());
            }
            
            // JWTFilter와 동일한 검증 로직 사용
            boolean isValidToken = jwtTokenProvider.isAccessTokenValid(token) || 
                                  (token != null && jwtUtil.isExpired(token) == false) ||
                                  (token != null && jwtUtil.isExpired(token) == false && 
                                   ("refresh".equals(jwtUtil.getCategory(token)) || "access".equals(jwtUtil.getCategory(token))));
            
            log.info("토큰 유효성 검사 결과: {}", isValidToken);
            
            if (isValidToken) {
                String userEmail = jwtUtil.getUserEmail(token);
                String userId = jwtUtil.getUserId(token);
                String userName = jwtUtil.getUsername(token);
                
                log.info("WebSocket connected user: email={}, userId={}, userName={}", userEmail, userId, userName);

                // 세션에 사용자 정보 저장
                accessor.setSessionAttributes(new HashMap<>());
                accessor.getSessionAttributes().put("userId", userId);
                accessor.getSessionAttributes().put("userName", userName);
                accessor.getSessionAttributes().put("userEmail", userEmail);
                
                log.info("세션에 사용자 정보 저장 완료: {}", accessor.getSessionAttributes());

                Authentication auth = new UsernamePasswordAuthenticationToken(
                    userEmail,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
                accessor.setUser(auth);
            } else {
                log.error("유효하지 않은 JWT 토큰입니다. WebSocket 연결을 거부합니다.");
                throw new IllegalArgumentException("JWT 인증 실패: 토큰이 유효하지 않습니다.");
            }
        } else {
            log.error("토큰이 null입니다. WebSocket 연결을 거부합니다.");
            throw new IllegalArgumentException("JWT 인증 실패: 쿠키에 토큰이 없습니다.");
        }
    }
    
    private String extractToken(StompHeaderAccessor accessor) {
        log.info("=== 토큰 추출 시작 ===");
        
        // 1. Authorization 헤더에서 토큰 추출 (우선순위 1)
        List<String> authorization = accessor.getNativeHeader("Authorization");
        log.info("Authorization 헤더: {}", authorization);
        if (authorization != null && !authorization.isEmpty()) {
            String authHeader = authorization.get(0);
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!token.isEmpty() && !token.equals("undefined")) {
                    log.info("Authorization 헤더에서 토큰 추출 성공");
                    return token;
                }
            }
        }
        
        // 2. 쿠키에서 토큰 추출 (우선순위 2)
        List<String> cookies = accessor.getNativeHeader("Cookie");
        log.info("WebSocket Cookies: {}", cookies);
        
        if (cookies != null && !cookies.isEmpty()) {
            String cookieHeader = cookies.get(0);
            log.info("원본 쿠키 헤더: {}", cookieHeader);
            
            String[] cookiePairs = cookieHeader.split(";");
            log.info("쿠키 페어 개수: {}", cookiePairs.length);
            
            for (String pair : cookiePairs) {
                String trimmedPair = pair.trim();
                log.info("쿠키 페어 처리: '{}'", trimmedPair);
                
                // JWTFilter와 동일한 쿠키 이름들 확인
                if (trimmedPair.startsWith("jwt_token=")) {
                    String token = trimmedPair.substring("jwt_token=".length());
                    if (!token.isEmpty() && !token.equals("undefined")) {
                        log.info("jwt_token 쿠키에서 토큰 추출 성공");
                        return token;
                    }
                } else if (trimmedPair.startsWith("access_token=")) {
                    String token = trimmedPair.substring("access_token=".length());
                    if (!token.isEmpty() && !token.equals("undefined")) {
                        log.info("access_token 쿠키에서 토큰 추출 성공");
                        return token;
                    }
                } else if (trimmedPair.startsWith("refresh=")) {
                    String token = trimmedPair.substring("refresh=".length());
                    if (!token.isEmpty() && !token.equals("undefined")) {
                        log.info("refresh 쿠키에서 토큰 추출 성공");
                        return token;
                    }
                }
            }
        } else {
            log.warn("쿠키가 null이거나 비어있습니다.");
        }
        
        // 3. 추가 헤더에서 토큰 추출 시도 (WebSocket 특화)
        List<String> xAuthToken = accessor.getNativeHeader("X-Auth-Token");
        log.info("X-Auth-Token 헤더: {}", xAuthToken);
        if (xAuthToken != null && !xAuthToken.isEmpty()) {
            String token = xAuthToken.get(0);
            if (!token.isEmpty() && !token.equals("undefined")) {
                log.info("X-Auth-Token 헤더에서 토큰 추출 성공");
                return token;
            }
        }
        
        // 4. 쿼리 파라미터에서 토큰 추출 시도
        String destination = accessor.getDestination();
        log.info("Destination: {}", destination);
        if (destination != null && destination.contains("token=")) {
            try {
                String[] parts = destination.split("token=");
                if (parts.length > 1) {
                    String token = parts[1].split("&")[0]; // & 이후 제거
                    if (!token.isEmpty() && !token.equals("undefined")) {
                        log.info("쿼리 파라미터에서 토큰 추출 성공");
                        return token;
                    }
                }
            } catch (Exception e) {
                log.warn("쿼리 파라미터에서 토큰 추출 실패: {}", e.getMessage());
            }
        }
        
        // 5. 모든 가능한 헤더 확인 (디버깅용)
        log.info("=== 모든 헤더 정보 ===");
        log.info("Authorization 헤더: {}", accessor.getNativeHeader("Authorization"));
        log.info("Cookie 헤더: {}", accessor.getNativeHeader("Cookie"));
        log.info("X-Auth-Token 헤더: {}", accessor.getNativeHeader("X-Auth-Token"));
        log.info("User-Agent 헤더: {}", accessor.getNativeHeader("User-Agent"));
        log.info("Origin 헤더: {}", accessor.getNativeHeader("Origin"));
        log.info("Referer 헤더: {}", accessor.getNativeHeader("Referer"));
        log.info("Host 헤더: {}", accessor.getNativeHeader("Host"));
        log.info("X-Forwarded-For 헤더: {}", accessor.getNativeHeader("X-Forwarded-For"));
        log.info("X-Forwarded-Proto 헤더: {}", accessor.getNativeHeader("X-Forwarded-Proto"));
        log.info("X-Real-IP 헤더: {}", accessor.getNativeHeader("X-Real-IP"));
        log.info("=== 헤더 정보 끝 ===");
        
        log.info("토큰을 찾을 수 없음");
        return null;
    }
    
    private void handleSend(StompHeaderAccessor accessor) {
        // 세션 정보가 없거나 userId가 없으면 토큰에서 복원 시도
        if (accessor.getSessionAttributes() == null || 
            accessor.getSessionAttributes().isEmpty() || 
            accessor.getSessionAttributes().get("userId") == null) {
            
            log.warn("세션 정보가 없거나 불완전합니다. 토큰에서 복원을 시도합니다. sessionAttributes={}", 
                    accessor.getSessionAttributes());
            
            String token = extractToken(accessor);
            
            if (token != null) {
                // JWTFilter와 동일한 검증 로직 사용
                boolean isValidToken = jwtTokenProvider.isAccessTokenValid(token) || 
                                      (token != null && jwtUtil.isExpired(token) == false) ||
                                      (token != null && jwtUtil.isExpired(token) == false && 
                                       ("refresh".equals(jwtUtil.getCategory(token)) || "access".equals(jwtUtil.getCategory(token))));
                
                if (isValidToken) {
                    String userEmail = jwtUtil.getUserEmail(token);
                    String userId = jwtUtil.getUserId(token);
                    String userName = jwtUtil.getUsername(token);
                    
                    log.info("토큰에서 사용자 정보 복원: email={}, userId={}, userName={}", userEmail, userId, userName);

                    // 세션에 사용자 정보 저장
                    if (accessor.getSessionAttributes() == null) {
                        accessor.setSessionAttributes(new HashMap<>());
                    }
                    accessor.getSessionAttributes().put("userId", userId);
                    accessor.getSessionAttributes().put("userName", userName);
                    accessor.getSessionAttributes().put("userEmail", userEmail);
                    
                    log.info("세션에 사용자 정보 복원 완료: {}", accessor.getSessionAttributes());
                } else {
                    log.error("토큰이 유효하지 않습니다. token={}, valid={}", token, isValidToken);
                }
            } else {
                log.error("토큰을 추출할 수 없습니다.");
            }
        } else {
            log.info("세션 정보가 이미 존재합니다: {}", accessor.getSessionAttributes());
        }
    }
}