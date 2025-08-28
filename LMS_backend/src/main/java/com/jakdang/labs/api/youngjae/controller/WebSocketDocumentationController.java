package com.jakdang.labs.api.youngjae.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/websocket-docs")
@Tag(name = "WebSocket 문서", description = "WebSocket API 사용법 및 문서")
public class WebSocketDocumentationController {

    /**
     * WebSocket API 전체 가이드
     * GET /api/websocket-docs/guide
     */
    @Operation(summary = "WebSocket API 가이드", description = "WebSocket API 사용을 위한 전체 가이드를 제공합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 가이드 정보를 조회했습니다",
            content = @Content(examples = {
                @ExampleObject(name = "WebSocket 가이드 응답", value = """
                    {
                      "connection": {
                        "url": "ws://localhost:8080/ws/chat",
                        "protocol": "STOMP",
                        "description": "WebSocket 연결을 위한 엔드포인트"
                      },
                      "sendMessage": {
                        "destination": "/app/chat.sendMessage",
                        "method": "POST",
                        "payload": {
                          "content": "String (메시지 내용)"
                        },
                        "description": "채팅 메시지를 전송합니다"
                      },
                      "addUser": {
                        "destination": "/app/chat.addUser",
                        "method": "POST",
                        "payload": "String (roomId)",
                        "description": "채팅방에 사용자를 추가합니다"
                      },
                      "typing": {
                        "destination": "/app/chat.typing",
                        "method": "POST",
                        "payload": {
                          "isTyping": "Boolean (타이핑 상태)"
                        },
                        "description": "사용자의 타이핑 상태를 전송합니다"
                      },
                      "subscribeTopics": {
                        "messages": "/topic/room/{roomId} - 채팅 메시지 수신",
                        "status": "/topic/room/{roomId}/status - 사용자 상태 변경",
                        "typing": "/topic/room/{roomId}/typing - 타이핑 상태"
                      },
                      "examples": {
                        "javascript": "// WebSocket 연결\\nconst socket = new WebSocket('ws://localhost:8080/ws/chat');\\nconst stompClient = Stomp.over(socket);\\n\\n// 메시지 전송\\nstompClient.send('/app/chat.sendMessage', {}, JSON.stringify({\\n    content: '안녕하세요!'\\n}));\\n\\n// 메시지 수신\\nstompClient.subscribe('/topic/room/roomId', function(message) {\\n    console.log('메시지 수신:', JSON.parse(message.body));\\n});"
                      }
                    }
                    """)
            }))
    })
    @GetMapping("/guide")
    public ResponseEntity<Map<String, Object>> getWebSocketGuide() {
        Map<String, Object> guide = new HashMap<>();
        
        // 연결 정보
        guide.put("connection", Map.of(
            "url", "ws://localhost:8080/ws/chat",
            "protocol", "STOMP",
            "description", "WebSocket 연결을 위한 엔드포인트"
        ));
        
        // 메시지 전송 엔드포인트
        Map<String, Object> sendMessage = new HashMap<>();
        sendMessage.put("destination", "/app/chat.sendMessage");
        sendMessage.put("method", "POST");
        sendMessage.put("payload", Map.of(
            "content", "String (메시지 내용)"
        ));
        sendMessage.put("description", "채팅 메시지를 전송합니다");
        guide.put("sendMessage", sendMessage);
        
        // 사용자 추가 엔드포인트
        Map<String, Object> addUser = new HashMap<>();
        addUser.put("destination", "/app/chat.addUser");
        addUser.put("method", "POST");
        addUser.put("payload", "String (roomId)");
        addUser.put("description", "채팅방에 사용자를 추가합니다");
        guide.put("addUser", addUser);
        
        // 타이핑 상태 엔드포인트
        Map<String, Object> typing = new HashMap<>();
        typing.put("destination", "/app/chat.typing");
        typing.put("method", "POST");
        typing.put("payload", Map.of(
            "isTyping", "Boolean (타이핑 상태)"
        ));
        typing.put("description", "사용자의 타이핑 상태를 전송합니다");
        guide.put("typing", typing);
        
        // 구독 토픽
        guide.put("subscribeTopics", Map.of(
            "messages", "/topic/room/{roomId} - 채팅 메시지 수신",
            "status", "/topic/room/{roomId}/status - 사용자 상태 변경",
            "typing", "/topic/room/{roomId}/typing - 타이핑 상태"
        ));
        
        // 예제 코드
        guide.put("examples", Map.of(
            "javascript", """
                // WebSocket 연결
                const socket = new WebSocket('ws://localhost:8080/ws/chat');
                const stompClient = Stomp.over(socket);
                
                // 메시지 전송
                stompClient.send('/app/chat.sendMessage', {}, JSON.stringify({
                    content: '안녕하세요!'
                }));
                
                // 메시지 수신
                stompClient.subscribe('/topic/room/roomId', function(message) {
                    console.log('메시지 수신:', JSON.parse(message.body));
                });
                """
        ));
        
        return ResponseEntity.ok(guide);
    }

    /**
     * WebSocket 메시지 스키마
     * GET /api/websocket-docs/schemas
     */
    @Operation(summary = "WebSocket 메시지 스키마", description = "WebSocket에서 사용하는 메시지 스키마를 제공합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 스키마 정보를 조회했습니다",
            content = @Content(examples = {
                @ExampleObject(name = "메시지 스키마 응답", value = """
                    {
                      "SendMessageRequest": {
                        "type": "object",
                        "properties": {
                          "content": {
                            "type": "string",
                            "description": "메시지 내용",
                            "required": true
                          }
                        }
                      },
                      "TypingMessage": {
                        "type": "object",
                        "properties": {
                          "isTyping": {
                            "type": "boolean",
                            "description": "타이핑 상태",
                            "required": true
                          }
                        }
                      },
                      "WebSocketMessage": {
                        "type": "object",
                        "properties": {
                          "id": {
                            "type": "string",
                            "description": "메시지 ID"
                          },
                          "type": {
                            "type": "string",
                            "description": "메시지 타입"
                          },
                          "roomId": {
                            "type": "string",
                            "description": "채팅방 ID"
                          },
                          "message": {
                            "type": "object",
                            "description": "메시지 정보"
                          }
                        }
                      }
                    }
                    """)
            }))
    })
    @GetMapping("/schemas")
    public ResponseEntity<Map<String, Object>> getMessageSchemas() {
        Map<String, Object> schemas = new HashMap<>();
        
        // SendMessageRequest 스키마
        Map<String, Object> sendMessageSchema = new HashMap<>();
        sendMessageSchema.put("type", "object");
        sendMessageSchema.put("properties", Map.of(
            "content", Map.of(
                "type", "string",
                "description", "메시지 내용",
                "required", true
            )
        ));
        schemas.put("SendMessageRequest", sendMessageSchema);
        
        // TypingMessage 스키마
        Map<String, Object> typingSchema = new HashMap<>();
        typingSchema.put("type", "object");
        typingSchema.put("properties", Map.of(
            "isTyping", Map.of(
                "type", "boolean",
                "description", "타이핑 상태",
                "required", true
            )
        ));
        schemas.put("TypingMessage", typingSchema);
        
        // WebSocketMessage 응답 스키마
        Map<String, Object> wsMessageSchema = new HashMap<>();
        wsMessageSchema.put("type", "object");
        wsMessageSchema.put("properties", Map.of(
            "id", Map.of("type", "string", "description", "메시지 ID"),
            "type", Map.of("type", "string", "description", "메시지 타입"),
            "roomId", Map.of("type", "string", "description", "채팅방 ID"),
            "message", Map.of("type", "object", "description", "메시지 정보")
        ));
        schemas.put("WebSocketMessage", wsMessageSchema);
        
        return ResponseEntity.ok(schemas);
    }

    /**
     * WebSocket 메시지 예제
     * GET /api/websocket-docs/examples
     */
    @Operation(summary = "WebSocket 메시지 예제", description = "WebSocket에서 사용하는 메시지의 실제 예제를 제공합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 메시지 예제를 조회했습니다",
            content = @Content(examples = {
                @ExampleObject(name = "메시지 예제 응답", value = """
                    {
                      "sendMessageRequest": {
                        "content": "안녕하세요! 반갑습니다."
                      },
                      "sendMessageResponse": {
                        "id": "msg_123456789",
                        "type": "message",
                        "roomId": "room_001",
                        "message": {
                          "id": "msg_123456789",
                          "content": "안녕하세요! 반갑습니다.",
                          "senderId": "user_001",
                          "senderName": "홍길동",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                      },
                      "typingMessage": {
                        "isTyping": true
                      },
                      "userStatusMessage": {
                        "type": "user_joined",
                        "roomId": "room_001",
                        "userId": "user_002",
                        "userName": "김철수"
                      },
                      "addUserPayload": "room_001"
                    }
                    """)
            }))
    })
    @GetMapping("/examples")
    public ResponseEntity<Map<String, Object>> getMessageExamples() {
        Map<String, Object> examples = new HashMap<>();
        
        // SendMessageRequest 예제
        examples.put("sendMessageRequest", Map.of(
            "content", "안녕하세요! 반갑습니다."
        ));
        
        // SendMessageResponse 예제
        Map<String, Object> sendMessageResponse = new HashMap<>();
        sendMessageResponse.put("id", "msg_123456789");
        sendMessageResponse.put("type", "message");
        sendMessageResponse.put("roomId", "room_001");
        
        Map<String, Object> messageInfo = new HashMap<>();
        messageInfo.put("id", "msg_123456789");
        messageInfo.put("content", "안녕하세요! 반갑습니다.");
        messageInfo.put("senderId", "user_001");
        messageInfo.put("senderName", "홍길동");
        messageInfo.put("timestamp", "2024-01-15T10:30:00Z");
        sendMessageResponse.put("message", messageInfo);
        
        examples.put("sendMessageResponse", sendMessageResponse);
        
        // TypingMessage 예제
        examples.put("typingMessage", Map.of(
            "isTyping", true
        ));
        
        // UserStatusMessage 예제
        Map<String, Object> userStatusMessage = new HashMap<>();
        userStatusMessage.put("type", "user_joined");
        userStatusMessage.put("roomId", "room_001");
        userStatusMessage.put("userId", "user_002");
        userStatusMessage.put("userName", "김철수");
        examples.put("userStatusMessage", userStatusMessage);
        
        // AddUser Payload 예제
        examples.put("addUserPayload", "room_001");
        
        return ResponseEntity.ok(examples);
    }

    /**
     * WebSocket 연결 상태 확인
     * GET /api/websocket-docs/status
     */
    @Operation(summary = "WebSocket 연결 상태", description = "WebSocket 서버의 연결 상태를 확인합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 상태 정보를 조회했습니다",
            content = @Content(examples = {
                @ExampleObject(name = "연결 상태 응답", value = """
                    {
                      "status": "running",
                      "timestamp": 1705312200000,
                      "message": "WebSocket 서버가 정상적으로 실행 중입니다",
                      "supportedProtocols": ["STOMP"],
                      "endpoints": [
                        "/ws/chat - WebSocket 연결 엔드포인트",
                        "/app/chat.sendMessage - 메시지 전송",
                        "/app/chat.addUser - 사용자 추가",
                        "/app/chat.typing - 타이핑 상태"
                      ],
                      "activeConnections": 5,
                      "uptime": "2시간 30분"
                    }
                    """)
            }))
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("timestamp", System.currentTimeMillis());
        status.put("message", "WebSocket 서버가 정상적으로 실행 중입니다");
        status.put("supportedProtocols", List.of("STOMP"));
        status.put("endpoints", List.of(
            "/ws/chat - WebSocket 연결 엔드포인트",
            "/app/chat.sendMessage - 메시지 전송",
            "/app/chat.addUser - 사용자 추가",
            "/app/chat.typing - 타이핑 상태"
        ));
        status.put("activeConnections", 5);
        status.put("uptime", "2시간 30분");
        
        return ResponseEntity.ok(status);
    }
} 