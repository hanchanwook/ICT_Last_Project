package com.jakdang.labs.config;

import com.jakdang.labs.websocket.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;
    private final AppConfig appConfig;
    
    // 배포 환경에서 웹소켓 연결을 위한 추가 설정
    @Override
    public void configureWebSocketTransport(org.springframework.web.socket.config.annotation.WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024) // 64KB
                   .setSendBufferSizeLimit(512 * 1024) // 512KB
                   .setSendTimeLimit(20000); // 20초
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 환경별 CORS 설정
        String[] allowedOrigins;
        if (appConfig.isDevMode()) {
            allowedOrigins = new String[]{
                "http://localhost:5173", 
                "http://localhost:7001", 
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:7001",
                "http://127.0.0.1:3000",
                "*"
            };
        } else {
            allowedOrigins = new String[]{
                "http://lmsync.site", 
                "https://lmsync.site",
                "http://www.lmsync.site",
                "https://www.lmsync.site",
                "http://*.lmsync.site",
                "https://*.lmsync.site",
                "*"
            };
        }
        
        log.info("WebSocket CORS 설정 - devMode: {}, allowedOrigins: {}", appConfig.isDevMode(), String.join(", ", allowedOrigins));
        
        // SockJS 설정으로 쿠키 전달 개선
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOrigins)
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
                .setWebSocketEnabled(true)
                .setSessionCookieNeeded(true)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000)
                .setHeartbeatTime(25 * 1000)
                .setStreamBytesLimit(512 * 1024);
        
        // 순수 WebSocket 지원 (Postman 테스트용)
        registry.addEndpoint("/ws/chat/websocket")
                .setAllowedOriginPatterns(allowedOrigins)
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
    
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}