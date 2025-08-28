package com.jakdang.labs.api.youngjae.controller;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.api.youngjae.dto.MemberSearchResponse;
import com.jakdang.labs.api.youngjae.dto.ChatRoomResponse;
import com.jakdang.labs.api.youngjae.dto.ChatRoomsResponse;
import com.jakdang.labs.api.youngjae.dto.CreateRoomRequest;
import com.jakdang.labs.api.youngjae.dto.CreateRoomResponse;
import com.jakdang.labs.api.youngjae.dto.LeaveRoomResponse;
import com.jakdang.labs.api.youngjae.dto.MessagesResponse;
import com.jakdang.labs.api.youngjae.dto.ParticipantsResponse;
import com.jakdang.labs.api.youngjae.dto.SendMessageRequest;
import com.jakdang.labs.api.youngjae.dto.SendMessageResponse;
import com.jakdang.labs.api.youngjae.dto.LastExitedAtResponse;
import com.jakdang.labs.api.youngjae.dto.UpdateExitedAtResponse;
import com.jakdang.labs.api.youngjae.dto.AddUserRequest;
import com.jakdang.labs.api.youngjae.dto.AddUserResponse;

import com.jakdang.labs.api.youngjae.service.ChatRoomService;
import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.api.auth.repository.AuthRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "채팅 API", description = "채팅방 및 메시지 관련 API")
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final JwtUtil jwtUtil;
    private final AuthRepository userRepository;

    /**
     * 쿠키에서 JWT 토큰 추출
     */
    private String getJwtTokenFromCookie(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출 시도
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 2. 쿠키에서 토큰 추출 시도
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * JWT 토큰에서 사용자 이메일 추출
     */
    private String getUserEmailFromToken(HttpServletRequest request) {
        String token = getJwtTokenFromCookie(request);
        log.info("추출된 JWT 토큰: {}", token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "null");
        
        if (token != null && !jwtUtil.isExpired(token)) {
            String email = jwtUtil.getUserEmail(token);
            log.info("JWT 토큰에서 추출한 사용자 이메일: {}", email);
            
            // 추가 디버깅 정보
            try {
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);
                String userId = jwtUtil.getUserId(token);
                log.info("JWT 토큰에서 추출한 추가 정보 - username: {}, role: {}, userId: {}", username, role, userId);
            } catch (Exception e) {
                log.error("JWT 토큰에서 추가 정보 추출 중 오류: {}", e.getMessage());
            }
            
            return email;
        } else {
            log.error("JWT 토큰이 null이거나 만료되었습니다. token={}, expired={}", 
                     token != null, token != null ? jwtUtil.isExpired(token) : "N/A");
        }
        return null;
    }

    /**
     * 사용중
     * 채팅방 목록 조회
     * GET /api/chat/rooms
     */
    @Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록을 조회합니다")
    @GetMapping("/rooms")
    public ResponseEntity<ChatRoomsResponse> getChatRooms(
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest httpRequest) {
        
        String userEmail = null;
        
        // 1. userId 파라미터가 있으면 userId로 사용자 이메일 조회
        if (userId != null && !userId.trim().isEmpty()) {
            try {
                // userId로 사용자 정보 조회하여 이메일 추출
                userEmail = getUserEmailFromUserId(userId);
                log.info("userId 파라미터로 사용자 이메일 조회: userId={}, email={}", userId, userEmail);
            } catch (Exception e) {
                log.error("userId로 사용자 이메일 조회 실패: userId={}, error={}", userId, e.getMessage());
            }
        }
        
        // 2. userId로 찾지 못했으면 JWT 토큰에서 사용자 이메일 자동 추출
        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = getUserEmailFromToken(httpRequest);
            log.info("JWT 토큰에서 사용자 이메일 추출: email={}", userEmail);
        }
        
        try {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                log.warn("사용자 이메일을 추출할 수 없습니다. userId={}", userId);
                return ResponseEntity.badRequest()
                    .body(ChatRoomsResponse.builder()
                        .success(false)
                        .error("인증되지 않은 사용자입니다.")
                        .rooms(List.of())
                        .build());
            }
            
            log.info("사용자 {}의 채팅방 목록 조회", userEmail);
            List<ChatRoomResponse> chatRooms = chatRoomService.getChatRoomsByUserEmail(userEmail);
            log.info("사용자 {}의 채팅방 목록 조회 완료: {}개", userEmail, chatRooms.size());
            
            return ResponseEntity.ok(ChatRoomsResponse.builder()
                .success(true)
                .rooms(chatRooms)
                .build());
            
        } catch (Exception e) {
            log.error("채팅방 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ChatRoomsResponse.builder()
                        .success(false)
                        .error("채팅방 목록 조회 중 오류가 발생했습니다.")
                        .rooms(List.of())
                        .build());
        }
    }
    
    /**
     * userId로 사용자 이메일 조회
     */
    private String getUserEmailFromUserId(String userId) {
        // UserEntity에서 userId로 사용자 정보 조회
        try {
            Optional<UserEntity> user = userRepository.findById(userId);
            if (user.isPresent()) {
                String email = user.get().getEmail();
                log.info("userId로 사용자 이메일 조회 성공: userId={}, email={}", userId, email);
                return email;
            } else {
                log.warn("userId에 해당하는 사용자를 찾을 수 없습니다: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("userId로 사용자 이메일 조회 실패: userId={}", userId, e);
        }
        return null;
    }
    
    /**
     * 사용중
     * 채팅방 생성
     * POST /api/chat/rooms
     */
    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다")
    @PostMapping("/rooms")
    public ResponseEntity<CreateRoomResponse> createChatRoom(
            @RequestBody CreateRoomRequest request,
            HttpServletRequest httpRequest) {
        
        // log.info("채팅방 생성 요청: request={}", request);
        
        // JWT 토큰에서 사용자 이메일 자동 추출
        String userEmail = getUserEmailFromToken(httpRequest);
        if (userEmail == null) {
            // log.error("JWT 토큰에서 사용자 이메일을 추출할 수 없습니다.");
            return ResponseEntity.badRequest()
                    .body(CreateRoomResponse.builder()
                            .success(false)
                            .error("인증되지 않은 사용자입니다.")
                            .room(null)
                            .build());
        }
        
                    // log.info("JWT 토큰에서 추출한 사용자 이메일: {}", userEmail);
        try {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                // log.error("채팅방 이름이 누락되었습니다.");
                return ResponseEntity.badRequest()
                        .body(CreateRoomResponse.builder()
                                .success(false)
                                .error("채팅방 이름이 필요합니다.")
                                .room(null)
                                .build());
            }
            
            if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
                // log.error("참여자 목록이 누락되었습니다.");
                return ResponseEntity.badRequest()
                        .body(CreateRoomResponse.builder()
                                .success(false)
                                .error("참여자가 필요합니다.")
                                .room(null)
                                .build());
            }
            
            CreateRoomResponse.RoomDto room = chatRoomService.createChatRoom(request, userEmail);
            
            // log.info("채팅방 생성 완료: {}", room.getId());
            
            return ResponseEntity.ok(CreateRoomResponse.builder()
                    .success(true)
                    .room(room)
                    .build());
            
        } catch (IllegalArgumentException e) {
            // log.error("채팅방 생성 중 유효성 검사 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CreateRoomResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .room(null)
                            .build());
        } catch (Exception e) {
            // log.error("채팅방 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(CreateRoomResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .room(null)
                            .build());
        }
    }
    
    /**
     * 사용중
     * 채팅방 메시지 목록 조회
     * GET /api/chat/rooms/{roomId}/messages
     */
    @Operation(summary = "채팅방 메시지 조회", description = "특정 채팅방의 메시지 목록을 조회합니다")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<MessagesResponse> getChatRoomMessages(
            @PathVariable String roomId,
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest httpRequest) {
        
        // JWT 토큰에서 사용자 이메일 자동 추출
        String userEmail = getUserEmailFromToken(httpRequest);
        log.info("getChatRoomMessages - 추출된 userEmail: {}", userEmail);
        
        if (userEmail == null) {
            log.error("getChatRoomMessages - userEmail이 null입니다.");
            return ResponseEntity.badRequest()
                    .body(MessagesResponse.builder()
                            .success(false)
                            .messages(List.of())
                            .build());
        }
        
        try {
            if (roomId == null || roomId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(MessagesResponse.builder()
                                .success(false)
                                .messages(List.of())
                                .build());
            }
            
            List<MessagesResponse.MessageDto> messages = chatRoomService.getChatRoomMessages(roomId, userEmail);
            
            // log.info("사용자 {}의 채팅방 {} 메시지 목록 조회 완료: {}개", userEmail, roomId, messages.size());
            
            return ResponseEntity.ok(MessagesResponse.builder()
                    .success(true)
                    .messages(messages)
                    .build());
            
        } catch (IllegalArgumentException e) {
            log.error("채팅방 메시지 조회 중 유효성 검사 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MessagesResponse.builder()
                            .success(false)
                            .messages(List.of())
                            .build());
        } catch (Exception e) {
            log.error("채팅방 메시지 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(MessagesResponse.builder()
                            .success(false)
                            .messages(List.of())
                            .build());
        }
    }
    
    /**
     * 사용중
     * 메시지 전송
     * POST /api/chat/rooms/{roomId}/messages
     */
    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송합니다")
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @PathVariable String roomId,
            @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        // JWT 토큰에서 사용자 이메일 자동 추출
        String userEmail = getUserEmailFromToken(httpRequest);
        try {
            if (roomId == null || roomId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(SendMessageResponse.builder()
                                .success(false)
                                .message(null)
                                .build());
            }
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(SendMessageResponse.builder()
                                .success(false)
                                .message(null)
                                .build());
            }
            
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(SendMessageResponse.builder()
                                .success(false)
                                .message(null)
                                .build());
            }
            
            SendMessageResponse.MessageDto message = chatRoomService.sendMessage(roomId, userEmail, request);
            
            // log.info("메시지 전송 완료: roomId={}, userEmail={}, messageId={}", roomId, userEmail, message.getId());
            
            return ResponseEntity.ok(SendMessageResponse.builder()
                    .success(true)
                    .message(message)
                    .build());
            
        } catch (IllegalArgumentException e) {
            // log.error("메시지 전송 중 유효성 검사 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(SendMessageResponse.builder()
                            .success(false)
                            .message(null)
                            .build());
        } catch (Exception e) {
            // log.error("메시지 전송 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(SendMessageResponse.builder()
                            .success(false)
                            .message(null)
                            .build());
        }
    }
    
    /**
     * 사용중
     * 사용자 검색 (채팅방 생성 시 사용)
     * GET /api/chat/users/search
     */
    @Operation(summary = "사용자 검색", description = "키워드로 사용자를 검색합니다")
    @GetMapping("/users/search")
    public ResponseEntity<MemberSearchResponse> searchUsers(@RequestParam(value = "keyword") String keyword, @RequestParam(value = "userId", required = false) String userId) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(MemberSearchResponse.builder()
                                .success(false)
                                .error("검색어를 입력해주세요")
                                .data(List.of())
                                .totalCount(0)
                                .build());
            }
            
            // Service 계층을 통해 사용자 검색 (이름만)
            List<MemberSearchResponse.UserDto> users = chatRoomService.searchUsers(keyword);
            
            // log.info("사용자 검색 완료: keyword={}, 결과={}개", keyword, users.size());
            
            return ResponseEntity.ok(MemberSearchResponse.builder()
                    .success(true)
                    .data(users)
                    .totalCount(users.size())
                    .build());
            
        } catch (Exception e) {
            // log.error("사용자 검색 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(MemberSearchResponse.builder()
                            .success(false)
                            .error("서버 내부 오류가 발생했습니다")
                            .data(List.of())
                            .totalCount(0)
                            .build());
        }
    }
    

    
    /**
     * 사용중
     * 채팅방 나가기
     * POST /api/chat/rooms/{roomId}/leave
     */
    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나갑니다")
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<LeaveRoomResponse> leaveChatRoom(
            @PathVariable String roomId,
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest httpRequest) {
        
        String userEmail = getUserEmailFromToken(httpRequest);
        if (userEmail == null) {
            return ResponseEntity.badRequest()
                .body(LeaveRoomResponse.builder()
                    .success(false)
                    .message("인증되지 않은 사용자입니다.")
                    .build());
        }
        
        // log.info("사용자 {}가 채팅방 {}에서 나가기 요청", userEmail, roomId);
        
        try {
            LeaveRoomResponse.LeaveRoomData data = chatRoomService.leaveChatRoom(roomId, userEmail);
            
            return ResponseEntity.ok(LeaveRoomResponse.builder()
                .success(true)
                .message("채팅방을 성공적으로 나갔습니다.")
                .data(data)
                .build());
        } catch (Exception e) {
            // log.error("채팅방 나가기 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(LeaveRoomResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
    
    /**
     * 사용중
     * 채팅방 참가자 목록 조회
     * GET /api/chat/rooms/{roomId}/participants
     */
    @Operation(summary = "채팅방 참가자 조회", description = "채팅방의 참가자 목록을 조회합니다")
    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<ParticipantsResponse> getChatRoomParticipants(@PathVariable String roomId) {
        // log.info("채팅방 {}의 참가자 목록 조회 요청", roomId);
        
        try {
            ParticipantsResponse.ParticipantsData data = chatRoomService.getChatRoomParticipants(roomId);
            return ResponseEntity.ok(ParticipantsResponse.builder()
                .success(true)
                .participants(data.getParticipants())
                .totalCount(data.getTotalCount())
                .build());
        } catch (Exception e) {
            // log.error("채팅방 참가자 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ParticipantsResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .participants(List.of())
                    .totalCount(0)
                    .build());
        }
    }
    
    /**
     * 사용중
     * 현재 로그인한 사용자의 해당 채팅방에서 마지막으로 나간 시간 조회
     * GET /api/chat/rooms/{roomId}/last-exited-at
     */
    @Operation(summary = "마지막 퇴장 시간 조회", description = "채팅방에서 마지막으로 퇴장한 시간을 조회합니다")
    @GetMapping("/rooms/{roomId}/last-exited-at")
    public ResponseEntity<LastExitedAtResponse> getLastExitedAt(
            @PathVariable String roomId,
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest httpRequest) {
        
        String userEmail = getUserEmailFromToken(httpRequest);
        if (userEmail == null) {
            return ResponseEntity.badRequest()
                .body(LastExitedAtResponse.builder()
                    .success(false)
                    .message("인증되지 않은 사용자입니다.")
                    .error("UNAUTHORIZED")
                    .build());
        }
        
        // log.info("사용자 {}의 채팅방 {} 마지막 나간 시간 조회 요청", userEmail, roomId);
        
        try {
            Instant lastExitedAt = chatRoomService.getLastExitedAt(roomId, userEmail);
            
            if (lastExitedAt == null) {
                return ResponseEntity.ok(LastExitedAtResponse.builder()
                    .success(true)
                    .lastExitedAt(null)
                    .message("채팅방 입장 기록이 없습니다")
                    .build());
            } else {
                return ResponseEntity.ok(LastExitedAtResponse.builder()
                    .success(true)
                    .lastExitedAt(lastExitedAt)
                    .message("마지막 나간 시간 조회 성공")
                    .build());
            }
            
        } catch (IllegalArgumentException e) {
            // log.error("마지막 나간 시간 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(LastExitedAtResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .error("ROOM_NOT_FOUND")
                    .build());
        } catch (Exception e) {
            // log.error("마지막 나간 시간 조회 중 예상치 못한 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(LastExitedAtResponse.builder()
                    .success(false)
                    .message("서버 내부 오류가 발생했습니다")
                    .error("INTERNAL_SERVER_ERROR")
                    .build());
        }
    }
    
    /**
     * 사용중
     * 현재 로그인한 사용자의 해당 채팅방에서 나간 시간을 현재 시간으로 업데이트
     * POST /api/chat/rooms/{roomId}/update-exited-at
     */
    @Operation(summary = "퇴장 시간 업데이트", description = "채팅방에서 퇴장한 시간을 현재 시간으로 업데이트합니다")
    @PostMapping("/rooms/{roomId}/update-exited-at")
    public ResponseEntity<UpdateExitedAtResponse> updateExitedAt(
            @PathVariable String roomId,
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest httpRequest) {
        
        String userEmail = getUserEmailFromToken(httpRequest);
        if (userEmail == null) {
            return ResponseEntity.badRequest()
                .body(UpdateExitedAtResponse.builder()
                    .success(false)
                    .message("인증되지 않은 사용자입니다.")
                    .error("UNAUTHORIZED")
                    .build());
        }
        
        // log.info("사용자 {}의 채팅방 {} exitedAt 업데이트 요청", userEmail, roomId);
        
        try {
            Instant updatedExitedAt = chatRoomService.updateExitedAt(roomId, userEmail);
            
            return ResponseEntity.ok(UpdateExitedAtResponse.builder()
                .success(true)
                .message("exitedAt 업데이트 성공")
                .exitedAt(updatedExitedAt)
                .build());
            
        } catch (IllegalArgumentException e) {
            // log.error("exitedAt 업데이트 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(UpdateExitedAtResponse.builder()
                    .success(false)
                    .message("exitedAt 업데이트 실패")
                    .error("ROOM_NOT_FOUND")
                    .build());
        } catch (Exception e) {
            // log.error("exitedAt 업데이트 중 예상치 못한 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(UpdateExitedAtResponse.builder()
                    .success(false)
                    .message("서버 내부 오류가 발생했습니다")
                    .error("INTERNAL_SERVER_ERROR")
                    .build());
        }
    }
    
    /**
     * 사용중
     * 채팅방에 사용자 추가
     * POST /api/chat/rooms/{roomId}/add-user
     */
    @Operation(summary = "채팅방 사용자 추가", description = "채팅방에 새로운 사용자를 추가합니다")
    @PostMapping("/rooms/{roomId}/add-user")
    public ResponseEntity<AddUserResponse> addUserToChatRoom(
            @PathVariable String roomId,
            @RequestBody AddUserRequest request,
            HttpServletRequest httpRequest) {
        
        String userEmail = getUserEmailFromToken(httpRequest);
        if (userEmail == null) {
            return ResponseEntity.badRequest()
                .body(AddUserResponse.builder()
                    .success(false)
                    .message("인증되지 않은 사용자입니다.")
                    .error("UNAUTHORIZED")
                    .build());
        }
        
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(AddUserResponse.builder()
                    .success(false)
                    .message("사용자 ID가 필요합니다.")
                    .error("USER_ID_REQUIRED")
                    .build());
        }
        
        // log.info("사용자 {}의 채팅방 {}에 사용자 {} 추가 요청", userEmail, roomId, request.getUserId());
        
        try {
            chatRoomService.addUserToChatRoom(roomId, request.getUserId(), userEmail);
            
            return ResponseEntity.ok(AddUserResponse.builder()
                .success(true)
                .message("사용자가 그룹에 추가되었습니다.")
                .build());
            
        } catch (IllegalArgumentException e) {
            // log.error("사용자 추가 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(AddUserResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .error("VALIDATION_ERROR")
                    .build());
        } catch (Exception e) {
            // log.error("사용자 추가 중 예상치 못한 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(AddUserResponse.builder()
                    .success(false)
                    .message("서버 내부 오류가 발생했습니다")
                    .error("INTERNAL_SERVER_ERROR")
                    .build());
        }
    }
}