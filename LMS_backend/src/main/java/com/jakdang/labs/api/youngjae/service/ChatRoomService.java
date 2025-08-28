package com.jakdang.labs.api.youngjae.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.entity.ChatGroupEntity;
import com.jakdang.labs.entity.ChatMessageEntity;
import com.jakdang.labs.entity.ChatRoomEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.youngjae.dto.ChatRoomResponse;
import com.jakdang.labs.api.youngjae.dto.CreateRoomRequest;
import com.jakdang.labs.api.youngjae.dto.CreateRoomResponse;
import com.jakdang.labs.api.youngjae.dto.LeaveRoomResponse;
import com.jakdang.labs.api.youngjae.dto.MessagesResponse;
import com.jakdang.labs.api.youngjae.dto.ParticipantsResponse;
import com.jakdang.labs.api.youngjae.dto.SendMessageRequest;
import com.jakdang.labs.api.youngjae.dto.SendMessageResponse;
import com.jakdang.labs.api.youngjae.repository.ChatGroupRepository;
import com.jakdang.labs.api.youngjae.repository.ChatMessageRepository;
import com.jakdang.labs.api.youngjae.repository.ChatRoomRepository;
import com.jakdang.labs.api.youngjae.repository.MemberRepository;
import com.jakdang.labs.api.youngjae.dto.MemberSearchResponse;
import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.api.auth.repository.AuthRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final AuthRepository userRepository;
    
    /**
     * 채팅방의 읽지 않은 메시지 수 계산 (공통 메서드)
     */
    private int calculateUnreadMessageCount(String roomId, ChatGroupEntity userChatGroup) {
        int unreadCount = 0;
        
        if (userChatGroup != null) {
            // exitedAt이 null이면 참여 중이므로 모든 메시지를 읽은 것으로 간주
            if (userChatGroup.getExitedAt() == null) {
                // log.info("채팅방 {} - 사용자가 참여 중이므로 모든 메시지를 읽은 것으로 간주", roomId);
                return 0;
            }
            
            // exitedAt 이후의 메시지 조회
            List<ChatMessageEntity> unreadMessages = chatMessageRepository
                .findByChatRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, userChatGroup.getExitedAt());
            
            // log.info("채팅방 {} - 퇴장 시간: {}, 퇴장 후 메시지 수: {}, 현재 사용자 ID: {}", 
            //     roomId, userChatGroup.getExitedAt(), unreadMessages.size(), userChatGroup.getId());
            
            // 자신이 보낸 메시지 제외한 개수 계산 (ID 기반)
            // log.info("채팅방 {} - 퇴장 후 메시지 상세 정보:", roomId);
            // for (ChatMessageEntity message : unreadMessages) {
            //     log.info("  메시지: messageId={}, senderId={}, content={}, createdAt={}", 
            //         message.getMessageId(), message.getId(), message.getContent(), message.getCreatedAt());
            // }
            
            // 중복 senderId 확인
            Set<String> allUniqueSenders = unreadMessages.stream()
                .map(ChatMessageEntity::getId)
                .filter(senderId -> !senderId.equals(userChatGroup.getId()))
                .collect(Collectors.toSet());
            // log.info("채팅방 {} - 고유한 발신자 수: {} (발신자 ID들: {})", roomId, allUniqueSenders.size(), allUniqueSenders);
            
            // 메시지 개수로 계산 (2배로 나오는 문제 해결을 위해 반으로 나눔)
            int rawCount = (int) unreadMessages.stream()
                .filter(message -> {
                    boolean isOwnMessage = message.getId().equals(userChatGroup.getId());
                    // if (isOwnMessage) {
                    //     log.info("자신이 보낸 메시지 제외: messageId={}, senderId={}, userChatGroupId={}", 
                    //         message.getMessageId(), message.getId(), userChatGroup.getId());
                    // } else {
                    //     log.info("다른 사람이 보낸 메시지: messageId={}, senderId={}, userChatGroupId={}", 
                    //         message.getMessageId(), message.getId(), userChatGroup.getId());
                    // }
                    return !isOwnMessage; // 자신이 보낸 메시지 제외
                })
                .count();
            
            unreadCount = rawCount / 2;
            // log.info("채팅방 {} - 원본 카운트: {}, 최종 카운트: {}", roomId, rawCount, unreadCount);
            
            // log.info("채팅방 {} - 읽지 않은 메시지 수: {}", roomId, unreadCount);
        } else {
            // log.info("채팅방 {} - 사용자 참여 정보를 찾을 수 없음", roomId);
        }
        
        return unreadCount;
    }

    /**
     * 사용자의 채팅방 목록 조회 (이메일 기반)
     */
    public List<ChatRoomResponse> getChatRoomsByUserEmail(String userEmail) {
        log.info("사용자 {}의 채팅방 목록 조회", userEmail);
        
        // 1. 이메일로 UserEntity 조회 (우선)
        Optional<UserEntity> user = userRepository.findByEmail(userEmail);
        if (user.isPresent()) {
            log.info("UserEntity에서 사용자 찾음: email={}, id={}", userEmail, user.get().getId());
        } else {
            log.warn("UserEntity에서 이메일 {}에 해당하는 사용자를 찾을 수 없습니다.", userEmail);
        }
        
        // 2. 이메일로 모든 MemberEntity 조회 (여러 명일 수 있음)
        List<MemberEntity> members = memberRepository.findAllByMemberEmail(userEmail);
        if (!members.isEmpty()) {
            log.info("MemberEntity에서 사용자 찾음: email={}, count={}", userEmail, members.size());
            for (MemberEntity member : members) {
                log.info("Member 정보: id={}, name={}, role={}, courseId={}", 
                    member.getId(), member.getMemberName(), member.getMemberRole(), member.getCourseId());
            }
        } else {
            log.warn("MemberEntity에서 이메일 {}에 해당하는 사용자를 찾을 수 없습니다.", userEmail);
        }
        
        // 3. 둘 다 없으면 빈 리스트 반환
        if (!user.isPresent() && members.isEmpty()) {
            log.error("이메일 {}에 해당하는 사용자를 UserEntity와 MemberEntity 모두에서 찾을 수 없습니다.", userEmail);
            return List.of();
        }
        
        // ChatGroup을 통해 사용자가 참여한 채팅방 ID 목록 조회 (이메일 기반)
        List<String> chatRoomIds = chatGroupRepository.findChatRoomIdsByMemberEmail(userEmail);
        
        // 채팅방 정보 조회 (Native SQL 사용)
        List<ChatRoomEntity> chatRooms = chatRoomRepository.findAllByChatRoomIds(chatRoomIds);
        
        // 각 채팅방의 마지막 메시지 조회 (null 안전 처리)
        Map<String, ChatMessageEntity> lastMessages = new HashMap<>();
        if (chatRoomIds != null && !chatRoomIds.isEmpty()) {
            for (String roomId : chatRoomIds) {
                if (roomId != null) {
                    try {
                        Optional<ChatMessageEntity> lastMessage = chatMessageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(roomId);
                        lastMessages.put(roomId, lastMessage.orElse(null));
                    } catch (Exception e) {
                        // log.warn("채팅방 {}의 마지막 메시지 조회 중 오류: {}", roomId, e.getMessage());
                    }
                }
            }
        }
        
        // 각 채팅방의 읽지 않은 메시지 수 계산
        Map<String, Integer> unreadCounts = new HashMap<>();
        for (String roomId : chatRoomIds) {
            if (roomId != null) {
                try {
                    // ChatGroup에서 사용자의 참여 정보 조회 (이메일 기반)
                    ChatGroupEntity userChatGroup = chatGroupRepository.findByChatRoomIdAndMemberEmail(roomId, userEmail);
                    
                    // log.info("채팅방 {} - 사용자 {}의 ChatGroup 조회 결과: {}", 
                    //     roomId, userEmail, userChatGroup != null ? "찾음" : "찾을 수 없음");
                    // if (userChatGroup != null) {
                    //     log.info("채팅방 {} - ChatGroup 정보: id={}, exitedAt={}, enteredAt={}", 
                    //         roomId, userChatGroup.getId(), userChatGroup.getExitedAt(), userChatGroup.getEnteredAt());
                    // }
                    
                    int unreadCount = calculateUnreadMessageCount(roomId, userChatGroup);
                    
                    unreadCounts.put(roomId, unreadCount);
                } catch (Exception e) {
                    // log.warn("채팅방 {}의 읽지 않은 메시지 수 계산 중 오류: {}", roomId, e.getMessage());
                    unreadCounts.put(roomId, 0);
                }
            }
        }
        
        return chatRooms.stream()
                .map(chatRoom -> {
                    ChatRoomResponse response = ChatRoomResponse.from(chatRoom, lastMessages.get(chatRoom.getChatRoomId()));
                    // 읽지 않은 메시지 수 설정
                    response.setUnread(unreadCounts.getOrDefault(chatRoom.getChatRoomId(), 0));
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 채팅방 목록 조회 (기존 메서드 - 호환성 유지)
     */
    public List<ChatRoomResponse> getChatRoomsByUserId(String userId) {
        // log.info("사용자 {}의 채팅방 목록 조회", userId);
        
        // ChatGroup을 통해 사용자가 참여한 채팅방 ID 목록 조회 (이메일 기반)
        List<String> chatRoomIds = chatGroupRepository.findChatRoomIdsByMemberEmail(userId);
        
        // 채팅방 정보 조회 (Native SQL 사용)
        List<ChatRoomEntity> chatRooms = chatRoomRepository.findAllByChatRoomIds(chatRoomIds);
        
        // 각 채팅방의 마지막 메시지 조회
        Map<String, ChatMessageEntity> lastMessages = chatRoomIds.stream()
                .collect(Collectors.toMap(
                    roomId -> roomId,
                    roomId -> chatMessageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(roomId).orElse(null)
                ));
        
        // 각 채팅방의 읽지 않은 메시지 수 계산
        Map<String, Integer> unreadCounts = new HashMap<>();
        for (String roomId : chatRoomIds) {
            if (roomId != null) {
                try {
                    // ChatGroup에서 사용자의 참여 정보 조회 (ID 기반)
                    List<ChatGroupEntity> allChatGroups = chatGroupRepository.findByChatRoomId(roomId);
                    // log.info("채팅방 {} - 전체 ChatGroup 수: {}", roomId, allChatGroups.size());
                    
                    // 모든 ChatGroup의 ID 출력
                    // allChatGroups.forEach(cg -> {
                    //     log.info("채팅방 {} - ChatGroup ID: {}", roomId, cg.getId());
                    // });
                    
                    List<ChatGroupEntity> userChatGroups = allChatGroups.stream()
                        .filter(cg -> cg.getId().equals(userId))
                        .collect(Collectors.toList());
                    
                    ChatGroupEntity userChatGroup = userChatGroups.isEmpty() ? null : userChatGroups.get(0);
                    
                    // log.info("채팅방 {} - 사용자 {}의 ChatGroup 조회 결과: {}", 
                    //     roomId, userId, userChatGroup != null ? "찾음" : "찾을 수 없음");
                    // if (userChatGroup != null) {
                    //     log.info("채팅방 {} - ChatGroup 정보: id={}, exitedAt={}, enteredAt={}", 
                    //         roomId, userChatGroup.getId(), userChatGroup.getExitedAt(), userChatGroup.getEnteredAt());
                    // }
                    
                    int unreadCount = calculateUnreadMessageCount(roomId, userChatGroup);
                    
                    unreadCounts.put(roomId, unreadCount);
                } catch (Exception e) {
                    // log.warn("채팅방 {}의 읽지 않은 메시지 수 계산 중 오류: {}", roomId, e.getMessage());
                    unreadCounts.put(roomId, 0);
                }
            }
        }
        
        return chatRooms.stream()
                .map(chatRoom -> {
                    ChatRoomResponse response = ChatRoomResponse.from(chatRoom, lastMessages.get(chatRoom.getChatRoomId()));
                    // 읽지 않은 메시지 수 설정
                    response.setUnread(unreadCounts.getOrDefault(chatRoom.getChatRoomId(), 0));
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 채팅방 생성 (새로운 API)
     */
    @Transactional
    public CreateRoomResponse.RoomDto createChatRoom(CreateRoomRequest request, String creatorEmail) {
        log.info("채팅방 생성 요청: request={}, creatorEmail={}", request, creatorEmail);
        
        // 참여자 유효성 검사 (중복 ID 제거)
        List<String> uniqueParticipantIds = request.getParticipantIds().stream()
                .distinct()
                .collect(Collectors.toList());
        
        log.info("참여자 ID 목록 (중복 제거): {}", uniqueParticipantIds);
        log.info("생성자 이메일: {}", creatorEmail);
        
        // 각 ID별로 첫 번째 Member 레코드만 선택
        List<MemberEntity> participants = uniqueParticipantIds.stream()
                .map(id -> {
                    List<MemberEntity> members = memberRepository.findByIdIn(List.of(id));
                    log.info("참여자 ID {} 조회 결과: {}", id, members.isEmpty() ? "찾을 수 없음" : "찾음");
                    return members.isEmpty() ? null : members.get(0);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        // 생성자는 자동으로 참여자에 포함 (중복 제거)
        MemberEntity creatorMember = null;
        if (creatorEmail != null) {
            // 이메일로 생성자 Member 조회 (여러 레코드가 있을 수 있음)
            List<MemberEntity> creatorMembers = memberRepository.findAllByMemberEmail(creatorEmail);
            log.info("생성자 이메일 {} 조회 결과: {}개 레코드", creatorEmail, creatorMembers.size());
            
            if (!creatorMembers.isEmpty()) {
                // 여러 레코드가 있는 경우 가장 적절한 레코드 선택
                if (creatorMembers.size() > 1) {
                    log.info("생성자 이메일 {}에 대해 {}개의 멤버 레코드를 찾았습니다.", creatorEmail, creatorMembers.size());
                    for (int i = 0; i < creatorMembers.size(); i++) {
                        MemberEntity member = creatorMembers.get(i);
                        log.info("  레코드 {}: memberId={}, memberRole={}, courseId={}, memberExpired={}", 
                            i + 1, member.getMemberId(), member.getMemberRole(), member.getCourseId(), member.getMemberExpired());
                    }
                    
                    // 만료되지 않은 레코드 중 첫 번째 선택
                    creatorMember = creatorMembers.stream()
                        .filter(m -> m.getMemberExpired() == null || m.getMemberExpired().isAfter(java.time.LocalDateTime.now()))
                        .findFirst()
                        .orElse(creatorMembers.get(0));
                    
                    log.info("선택된 생성자 레코드: memberId={}, memberRole={}, courseId={}", 
                        creatorMember.getMemberId(), creatorMember.getMemberRole(), creatorMember.getCourseId());
                } else {
                    creatorMember = creatorMembers.get(0);
                }
                
                String creatorId = creatorMember.getId();
                log.info("생성자가 참여자 목록에 포함되어 있는가?: {}", uniqueParticipantIds.contains(creatorId));
                
                if (!uniqueParticipantIds.contains(creatorId)) {
                    participants.add(0, creatorMember); // 생성자를 첫 번째로 추가
                    log.info("생성자 {}가 참여자 목록에 자동 추가됨", creatorEmail);
                } else {
                    log.info("생성자 {}가 이미 참여자 목록에 포함되어 있습니다.", creatorEmail);
                }
            } else {
                log.warn("생성자 이메일 {}에 해당하는 Member를 찾을 수 없습니다.", creatorEmail);
            }
        } else {
            log.warn("생성자 이메일이 null입니다.");
        }
        
        log.info("최종 참여자 목록: {}명", participants.size());
        log.info("참여자 ID 목록: {}", participants.stream().map(MemberEntity::getId).collect(Collectors.toList()));
        
        // 채팅방 생성 (UUID는 JPA가 자동 생성)
        String creatorId = null;
        if (creatorMember != null) {
            creatorId = creatorMember.getId(); // 사용자 ID 사용
        }
        
        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .id(creatorId) // 생성자 사용자 ID로 설정
                .chatRoomName(request.getName())
                .memberCount(participants.size())
                .createdAt(Instant.now())
                .build();
        
        ChatRoomEntity savedChatRoom = chatRoomRepository.save(chatRoom);
        log.info("채팅방 생성 완료: chatRoomId={}, creatorId={}", savedChatRoom.getChatRoomId(), creatorId);
        
        // 채팅 그룹 생성 (참여자 추가)
        log.info("채팅 그룹 생성 시작: {}명의 참여자", participants.size());
        for (MemberEntity participant : participants) {
            ChatGroupEntity chatGroup = ChatGroupEntity.builder()
                    .id(participant.getMemberId()) // 각 참여자의 memberId를 ChatGroup의 id 필드에 저장
                    .chatRoomId(savedChatRoom.getChatRoomId())
                    .createdAt(Instant.now())
                    .enteredAt(Instant.now())
                    .build();
            
            ChatGroupEntity savedChatGroup = chatGroupRepository.save(chatGroup);
            log.info("채팅 그룹 생성 완료: participantId={}, memberId={}, chatRoomId={}, chatGroupId={}", 
                    participant.getId(), participant.getMemberId(), savedChatRoom.getChatRoomId(), savedChatGroup.getChatGroupId());
        }
        
        return CreateRoomResponse.RoomDto.from(savedChatRoom, participants);
    }
    
    /**
     * 채팅방 생성 (기존 메서드)
     */
    public ChatRoomResponse createChatRoom(String creatorId, String chatRoomName) {
        // log.info("채팅방 생성: 생성자: {}, 이름: {}", creatorId, chatRoomName);
        
        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .id(creatorId)
                .chatRoomName(chatRoomName)
                .memberCount(1) // 생성자 포함
                .build();
        
        ChatRoomEntity savedChatRoom = chatRoomRepository.save(chatRoom);
        return ChatRoomResponse.from(savedChatRoom, null); // 새로 생성된 채팅방이므로 마지막 메시지는 없음
    }
    
    /**
     * 채팅방 조회
     */
    public ChatRoomEntity getChatRoomById(String chatRoomId) {
        return chatRoomRepository.findByChatRoomId(chatRoomId);
    }
    
    /**
     * 채팅방 메시지 목록 조회 (사용자 입장 시간 이후 메시지만)
     * @param roomId 채팅방 ID
     * @param userEmail 사용자 이메일
     * @return 메시지 목록
     */
    public List<MessagesResponse.MessageDto> getChatRoomMessages(String roomId, String userEmail) {
        log.info("사용자 {}의 채팅방 {} 메시지 목록 조회", userEmail, roomId);
        
        // 채팅방 존재 여부 확인
        ChatRoomEntity chatRoom = chatRoomRepository.findByChatRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("존재하지 않는 채팅방입니다.");
        }
        
        // 사용자 존재 여부 확인 (MemberEntity에서 먼저 찾기)
        // 동일한 이메일을 가진 여러 레코드가 있을 수 있으므로 모든 레코드를 조회
        List<MemberEntity> members = memberRepository.findAllByMemberEmail(userEmail);
        if (members.isEmpty()) {
            // MemberEntity에서 찾지 못하면 UserEntity에서 찾기
            log.info("MemberEntity에서 사용자를 찾지 못했습니다. UserEntity에서 찾아보겠습니다. userEmail: {}", userEmail);
            Optional<UserEntity> user = userRepository.findByEmail(userEmail);
            if (!user.isPresent()) {
                log.error("UserEntity에서도 사용자를 찾지 못했습니다. userEmail: {}", userEmail);
                throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
            }
            log.info("UserEntity에서 사용자를 찾았습니다. userId: {}", user.get().getId());
        } else {
            // 여러 레코드가 있는 경우 로깅
            log.info("MemberEntity에서 동일한 이메일을 가진 {}개의 레코드를 찾았습니다. userEmail: {}", members.size(), userEmail);
            for (int i = 0; i < members.size(); i++) {
                MemberEntity member = members.get(i);
                log.info("  레코드 {}: memberId={}, memberRole={}, courseId={}, memberExpired={}", 
                    i + 1, member.getId(), member.getMemberRole(), member.getCourseId(), member.getMemberExpired());
            }
            
            // 가장 적절한 레코드 선택 (만료되지 않은 레코드 중 첫 번째)
            MemberEntity selectedMember = members.stream()
                .filter(m -> m.getMemberExpired() == null || m.getMemberExpired().isAfter(java.time.LocalDateTime.now()))
                .findFirst()
                .orElse(members.get(0)); // 만료되지 않은 레코드가 없으면 첫 번째 레코드 사용
            
            log.info("선택된 레코드: memberId={}, memberRole={}, courseId={}", 
                selectedMember.getId(), selectedMember.getMemberRole(), selectedMember.getCourseId());
        }
        
        // 사용자의 채팅방 입장 정보 조회
        ChatGroupEntity chatGroup = chatGroupRepository.findByChatRoomIdAndMemberEmail(roomId, userEmail);
        if (chatGroup == null) {
            throw new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다.");
        }
        
        // 사용자 입장 시간 이후의 메시지 목록 조회
        Instant joinedAt = chatGroup.getCreatedAt();
        List<ChatMessageEntity> messages = chatMessageRepository.findByChatRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, joinedAt);
        
        // log.info("사용자 {}의 채팅방 {} 입장 시간: {}, 조회된 메시지 수: {}", userEmail, roomId, joinedAt, messages.size());
        
        // 발신자 정보 조회 (동일한 ID를 가진 여러 레코드가 있을 수 있으므로 처리)
        List<String> senderIds = messages.stream()
                .map(ChatMessageEntity::getId)
                .distinct()
                .collect(Collectors.toList());
        
        final Map<String, MemberEntity> users;
        if (!senderIds.isEmpty()) {
            // 각 ID별로 모든 레코드를 조회하고 적절한 레코드 선택
            Map<String, MemberEntity> userMap = new HashMap<>();
            
            for (String senderId : senderIds) {
                List<MemberEntity> membersWithSameId = memberRepository.findByIdIn(List.of(senderId));
                if (!membersWithSameId.isEmpty()) {
                    // 여러 레코드가 있는 경우 로깅
                    if (membersWithSameId.size() > 1) {
                        log.info("동일한 ID를 가진 {}개의 레코드를 찾았습니다. senderId: {}", membersWithSameId.size(), senderId);
                        for (int i = 0; i < membersWithSameId.size(); i++) {
                            MemberEntity member = membersWithSameId.get(i);
                            log.info("  레코드 {}: memberId={}, memberRole={}, courseId={}, memberExpired={}", 
                                i + 1, member.getId(), member.getMemberRole(), member.getCourseId(), member.getMemberExpired());
                        }
                    }
                    
                    // 가장 적절한 레코드 선택 (만료되지 않은 레코드 중 첫 번째)
                    MemberEntity selectedMember = membersWithSameId.stream()
                        .filter(m -> m.getMemberExpired() == null || m.getMemberExpired().isAfter(java.time.LocalDateTime.now()))
                        .findFirst()
                        .orElse(membersWithSameId.get(0)); // 만료되지 않은 레코드가 없으면 첫 번째 레코드 사용
                    
                    userMap.put(senderId, selectedMember);
                }
            }
            users = userMap;
        } else {
            users = new HashMap<>();
        }
        
        // 메시지 DTO 변환
        return messages.stream()
            .map(message -> {
                MemberEntity sender = users.get(message.getId()); 
                return MessagesResponse.MessageDto.from(message, sender);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 메시지 전송
     */
    @Transactional
    public SendMessageResponse.MessageDto sendMessage(String roomId, String senderEmail, SendMessageRequest request) {
        log.info("채팅방 {}에서 사용자 {}가 메시지 전송", roomId, senderEmail);
        
        // 채팅방 존재 여부 확인
        ChatRoomEntity chatRoom = chatRoomRepository.findByChatRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("존재하지 않는 채팅방입니다.");
        }
        
        // 이메일로 발신자 존재 여부 확인 (MemberEntity에서 먼저 찾기)
        // 동일한 이메일을 가진 여러 레코드가 있을 수 있으므로 모든 레코드를 조회
        List<MemberEntity> members = memberRepository.findAllByMemberEmail(senderEmail);
        MemberEntity sender;
        
        if (members.isEmpty()) {
            // MemberEntity에서 찾지 못하면 UserEntity에서 찾기
            log.info("MemberEntity에서 발신자를 찾지 못했습니다. UserEntity에서 찾아보겠습니다. senderEmail: {}", senderEmail);
            Optional<UserEntity> userOpt = userRepository.findByEmail(senderEmail);
            if (!userOpt.isPresent()) {
                log.error("UserEntity에서도 발신자를 찾지 못했습니다. senderEmail: {}", senderEmail);
                throw new RuntimeException("해당 이메일의 회원을 찾을 수 없습니다: " + senderEmail);
            }
            log.info("UserEntity에서 발신자를 찾았습니다. userId: {}", userOpt.get().getId());
            // UserEntity를 MemberEntity로 변환하는 로직이 필요하지만, 일단 예외를 발생시킵니다.
            throw new IllegalArgumentException("채팅 기능은 MemberEntity에 등록된 사용자만 사용할 수 있습니다.");
        } else {
            // 여러 레코드가 있는 경우 로깅
            log.info("MemberEntity에서 동일한 이메일을 가진 {}개의 레코드를 찾았습니다. senderEmail: {}", members.size(), senderEmail);
            for (int i = 0; i < members.size(); i++) {
                MemberEntity member = members.get(i);
                log.info("  레코드 {}: memberId={}, memberRole={}, courseId={}, memberExpired={}", 
                    i + 1, member.getId(), member.getMemberRole(), member.getCourseId(), member.getMemberExpired());
            }
            
            // 가장 적절한 레코드 선택 (만료되지 않은 레코드 중 첫 번째)
            sender = members.stream()
                .filter(m -> m.getMemberExpired() == null || m.getMemberExpired().isAfter(java.time.LocalDateTime.now()))
                .findFirst()
                .orElse(members.get(0)); // 만료되지 않은 레코드가 없으면 첫 번째 레코드 사용
            
            log.info("선택된 발신자 레코드: memberId={}, memberRole={}, courseId={}", 
                sender.getId(), sender.getMemberRole(), sender.getCourseId());
        }
        
        String senderId = sender.getId();
        
        // 채팅방 참여 여부 확인 (이메일 기반)
        long participantCount = chatGroupRepository.countByChatRoomIdAndMemberEmail(roomId, senderEmail);
        if (participantCount == 0) {
            throw new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다.");
        }
        
        // 메시지 생성 및 저장 (UUID는 JPA가 자동 생성)
        ChatMessageEntity message = ChatMessageEntity.builder()
                .id(senderId)
                .chatRoomId(roomId)
                .content(request.getContent())
                .createdAt(Instant.now())
                .build();
        
        ChatMessageEntity savedMessage = chatMessageRepository.save(message);
        
        return SendMessageResponse.MessageDto.from(savedMessage, sender);
    }
    
    /**
     * 사용자 검색 (채팅방 생성 시 사용)
     * @param keyword 검색 키워드 (이름만)
     * @return 검색된 사용자 목록
     */
    public List<MemberSearchResponse.UserDto> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 키워드는 필수입니다.");
        }
        
        List<MemberEntity> members = memberRepository
                .findByMemberNameContainingIgnoreCase(keyword);
        
        return members.stream()
                .map(MemberSearchResponse.UserDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 이메일로 사용자 검색
     * @param email 검색할 이메일
     * @return 검색된 사용자 정보
     */
    public Optional<MemberSearchResponse.UserDto> searchUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        
        return memberRepository.findByMemberEmailIgnoreCase(email)
                .map(MemberSearchResponse.UserDto::from);
    }
    
    /**
     * 채팅방 나가기
     * @param roomId 채팅방 ID
     * @param userEmail 사용자 이메일
     * @return 나가기 결과 데이터
     */
    @Transactional
    public LeaveRoomResponse.LeaveRoomData leaveChatRoom(String roomId, String userEmail) {
        // log.info("채팅방 나가기 요청: roomId={}, userEmail={}", roomId, userEmail);
        
        // 채팅방 존재 여부 확인
        ChatRoomEntity chatRoom = chatRoomRepository.findByChatRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("존재하지 않는 채팅방입니다.");
        }
        
        // 사용자 존재 여부 확인 (동일한 이메일을 가진 여러 레코드가 있을 수 있으므로 모든 레코드를 조회)
        List<MemberEntity> members = memberRepository.findAllByMemberEmail(userEmail);
        if (members.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        
        // 여러 레코드가 있는 경우 로깅
        if (members.size() > 1) {
            log.info("MemberEntity에서 동일한 이메일을 가진 {}개의 레코드를 찾았습니다. userEmail: {}", members.size(), userEmail);
            for (int i = 0; i < members.size(); i++) {
                MemberEntity member = members.get(i);
                log.info("  레코드 {}: memberId={}, memberRole={}, courseId={}, memberExpired={}", 
                    i + 1, member.getId(), member.getMemberRole(), member.getCourseId(), member.getMemberExpired());
            }
        }
        
        // 가장 적절한 레코드 선택 (만료되지 않은 레코드 중 첫 번째)
        MemberEntity member = members.stream()
            .filter(m -> m.getMemberExpired() == null || m.getMemberExpired().isAfter(java.time.LocalDateTime.now()))
            .findFirst()
            .orElse(members.get(0)); // 만료되지 않은 레코드가 없으면 첫 번째 레코드 사용
        
        String memberId = member.getId();
        
        // 채팅방 참여 여부 확인
        long participantCount = chatGroupRepository.countByChatRoomIdAndMemberEmail(roomId, userEmail);
        if (participantCount == 0) {
            throw new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다.");
        }
        
        // ChatGroup에서 사용자의 참여 정보 조회 (이메일 기반)
        ChatGroupEntity chatGroup = chatGroupRepository.findByChatRoomIdAndMemberEmail(roomId, userEmail);
        if (chatGroup == null) {
            throw new IllegalArgumentException("채팅방 참여 정보를 찾을 수 없습니다.");
        }
        
        // 나간 시간 기록
        Instant exitTime = Instant.now();
        chatGroup.setExitedAt(exitTime);
        chatGroupRepository.save(chatGroup);
        // log.info("사용자 {}의 채팅방 {} 나간 시간 기록: {}", userEmail, roomId, exitTime);
        
        // 채팅방에서 완전히 나가기 (레코드 삭제)
        chatGroupRepository.delete(chatGroup);
        
        // 채팅방 멤버 수 감소
        int newMemberCount = chatRoom.getMemberCount() - 1;
        chatRoom.setMemberCount(newMemberCount);
        
        // memberCount가 0이 되면 채팅방과 관련 데이터 삭제
        if (newMemberCount <= 0) {
            // log.info("채팅방 {}의 멤버 수가 0이 되어 채팅방을 삭제합니다.", roomId);
            
            // 채팅방의 모든 메시지 삭제
            List<ChatMessageEntity> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId);
            if (!messages.isEmpty()) {
                chatMessageRepository.deleteAll(messages);
                // log.info("채팅방 {}의 메시지 {}개를 삭제했습니다.", roomId, messages.size());
            }
            
            // 채팅방 삭제
            chatRoomRepository.delete(chatRoom);
            // log.info("채팅방 {}를 삭제했습니다.", roomId);
            
            return LeaveRoomResponse.LeaveRoomData.builder()
                    .roomId(roomId)
                    .leftAt(Instant.now())
                    .build();
        } else {
            // 멤버 수가 0보다 크면 채팅방 정보 업데이트
            chatRoomRepository.save(chatRoom);
            // log.info("사용자 {}가 채팅방 {}에서 성공적으로 나갔습니다. (남은 멤버 수: {})", userEmail, roomId, newMemberCount);
            
            return LeaveRoomResponse.LeaveRoomData.builder()
                    .roomId(roomId)
                    .leftAt(Instant.now())
                    .build();
        }
    }
    
    /**
     * 채팅방 참가자 목록 조회
     * @param roomId 채팅방 ID
     * @return 참가자 목록 데이터
     */
    public ParticipantsResponse.ParticipantsData getChatRoomParticipants(String roomId) {
        log.info("채팅방 {}의 참가자 목록 조회", roomId);
        
        // 채팅방 존재 여부 확인
        ChatRoomEntity chatRoom = chatRoomRepository.findByChatRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("존재하지 않는 채팅방입니다.");
        }
        
        // 채팅방의 참가자 목록 조회
        List<ChatGroupEntity> chatGroups = chatGroupRepository.findByChatRoomId(roomId);
        log.info("채팅방 {}에서 {}개의 ChatGroup 레코드를 찾았습니다.", roomId, chatGroups.size());
        
        // 참가자 memberId 목록 추출 (ChatGroupEntity.id는 이제 memberId를 저장)
        List<String> participantMemberIds = chatGroups.stream()
                .map(ChatGroupEntity::getId)
                .collect(Collectors.toList());
        
        log.info("참가자 memberId 목록: {}", participantMemberIds);
        
        // 참가자 정보 조회 (memberId로 직접 조회)
        List<MemberEntity> participants = new ArrayList<>();
        
        for (String memberId : participantMemberIds) {
            Optional<MemberEntity> memberOpt = memberRepository.findByMemberId(memberId);
            if (memberOpt.isPresent()) {
                MemberEntity member = memberOpt.get();
                log.info("참가자 정보 조회: memberId={}, userId={}, name={}, email={}, role={}", 
                    member.getMemberId(), member.getId(), member.getMemberName(), member.getMemberEmail(), member.getMemberRole());
                participants.add(member);
            } else {
                log.warn("memberId {}에 해당하는 Member를 찾을 수 없습니다.", memberId);
            }
        }
        
        // DTO 변환
        List<ParticipantsResponse.ParticipantDto> participantDtos = participants.stream()
                .map(member -> {
                    // 해당 참가자의 참여 정보 찾기 (memberId로 매칭)
                    ChatGroupEntity chatGroup = chatGroups.stream()
                            .filter(cg -> cg.getId().equals(member.getMemberId()))
                            .findFirst()
                            .orElse(null);
                    
                    return ParticipantsResponse.ParticipantDto.builder()
                            .id(member.getId()) // 사용자 ID 반환
                            .name(member.getMemberName())
                            .email(member.getMemberEmail())
                            .role(member.getMemberRole())
                            .isOnline(true) // TODO: 실제 온라인 상태 확인 로직 구현 필요
                            .joinedAt(chatGroup != null ? chatGroup.getCreatedAt() : Instant.now())
                            .build();
                })
                .collect(Collectors.toList());
        
        log.info("채팅방 {}의 참가자 수: {}", roomId, participantDtos.size());
        
        return ParticipantsResponse.ParticipantsData.builder()
                .participants(participantDtos)
                .totalCount(participantDtos.size())
                .build();
    }
    
    /**
     * 학생/교사 구분 없이 통합 검색 (채팅방 생성 시 사용)
     * @param keyword 검색 키워드 (이름만)
     * @return 검색된 사용자 목록 (학생, 교사 모두 포함)
     */
    
    /**
     * 사용자의 채팅방 마지막 나간 시간 조회
     * @param roomId 채팅방 ID
     * @param userEmail 사용자 이메일
     * @return 마지막 나간 시간 (없으면 null)
     */
    public Instant getLastExitedAt(String roomId, String userEmail) {
        // log.info("사용자 {}의 채팅방 {} 마지막 나간 시간 조회", userEmail, roomId);
        
        // 채팅방 존재 여부 확인
        ChatRoomEntity chatRoom = chatRoomRepository.findByChatRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("채팅방을 찾을 수 없습니다");
        }
        
        // 사용자 존재 여부 확인 (동일한 이메일을 가진 여러 레코드가 있을 수 있으므로 모든 레코드를 조회)
        List<MemberEntity> members = memberRepository.findAllByMemberEmail(userEmail);
        if (members.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        
        // 여러 레코드가 있는 경우 로깅
        if (members.size() > 1) {
            log.info("MemberEntity에서 동일한 이메일을 가진 {}개의 레코드를 찾았습니다. userEmail: {}", members.size(), userEmail);
            for (int i = 0; i < members.size(); i++) {
                MemberEntity member = members.get(i);
                log.info("  레코드 {}: memberId={}, memberRole={}, courseId={}, memberExpired={}", 
                    i + 1, member.getId(), member.getMemberRole(), member.getCourseId(), member.getMemberExpired());
            }
        }
        
        // ChatGroup에서 해당 사용자의 참여 정보 조회
        ChatGroupEntity chatGroup = chatGroupRepository.findByChatRoomIdAndMemberEmail(roomId, userEmail);
        
        // 참여 정보가 없으면 null 반환 (입장 기록이 없음)
        if (chatGroup == null) {
            // log.info("사용자 {}의 채팅방 {} 입장 기록이 없습니다", userEmail, roomId);
            return null;
        }
        
        // 마지막 나간 시간 반환 (exitedAt 필드 사용)
        Instant lastExitedAt = chatGroup.getExitedAt();
        // log.info("사용자 {}의 채팅방 {} 마지막 나간 시간: {}", userEmail, roomId, lastExitedAt);
        
        return lastExitedAt;
    }
    
    /**
     * 사용자의 채팅방 나간 시간을 현재 시간으로 업데이트
     * @param roomId 채팅방 ID
     * @param userEmail 사용자 이메일
     * @return 업데이트된 나간 시간
     */
    @Transactional
    public Instant updateExitedAt(String roomId, String userEmail) {
        // log.info("사용자 {}의 채팅방 {} 나간 시간 업데이트", userEmail, roomId);
        
        // 채팅방 존재 여부 확인
        ChatRoomEntity chatRoom = chatRoomRepository.findByChatRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("채팅방을 찾을 수 없습니다");
        }
        
        // 사용자 존재 여부 확인 (동일한 이메일을 가진 여러 레코드가 있을 수 있으므로 모든 레코드를 조회)
        List<MemberEntity> members = memberRepository.findAllByMemberEmail(userEmail);
        if (members.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        
        // 여러 레코드가 있는 경우 로깅
        if (members.size() > 1) {
            log.info("MemberEntity에서 동일한 이메일을 가진 {}개의 레코드를 찾았습니다. userEmail: {}", members.size(), userEmail);
            for (int i = 0; i < members.size(); i++) {
                MemberEntity member = members.get(i);
                log.info("  레코드 {}: memberId={}, memberRole={}, courseId={}, memberExpired={}", 
                    i + 1, member.getId(), member.getMemberRole(), member.getCourseId(), member.getMemberExpired());
            }
        }
        
        // ChatGroup에서 해당 사용자의 참여 정보 조회
        ChatGroupEntity chatGroup = chatGroupRepository.findByChatRoomIdAndMemberEmail(roomId, userEmail);
        
        // 참여 정보가 없으면 예외 발생
        if (chatGroup == null) {
            throw new IllegalArgumentException("채팅방 참여 정보를 찾을 수 없습니다");
        }
        
        // 현재 시간으로 exitedAt 업데이트
        Instant currentTime = Instant.now();
        chatGroup.setExitedAt(currentTime);
        chatGroupRepository.save(chatGroup);
        
        // log.info("사용자 {}의 채팅방 {} 나간 시간 업데이트 완료: {}", userEmail, roomId, currentTime);
        
        return currentTime;
    }
    
    /**
     * 채팅방에 사용자 추가
     * @param roomId 채팅방 ID
     * @param userId 추가할 사용자 ID
     * @param addedByEmail 추가를 요청한 사용자 이메일
     */
    @Transactional
    public void addUserToChatRoom(String roomId, String userId, String addedByEmail) {
        // log.info("채팅방 {}에 사용자 {} 추가 요청 (요청자: {})", roomId, userId, addedByEmail);
        
        // 채팅방 존재 여부 확인
        ChatRoomEntity chatRoom = chatRoomRepository.findByChatRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("존재하지 않는 채팅방입니다.");
        }
        
        // 추가할 사용자 존재 여부 확인
        // log.info("사용자 ID로 조회 시도: {}", userId);
        Optional<MemberEntity> memberToAdd = memberRepository.findByUserId(userId);
        if (!memberToAdd.isPresent()) {
            // log.error("사용자 ID {}로 조회된 사용자가 없습니다.", userId);
            throw new IllegalArgumentException("추가할 사용자를 찾을 수 없습니다.");
        }
        // log.info("사용자 조회 성공: {}", memberToAdd.get().getMemberName());
        
        // 요청자 존재 여부 확인 (동일한 이메일을 가진 여러 레코드가 있을 수 있으므로 모든 레코드를 조회)
        List<MemberEntity> requesters = memberRepository.findAllByMemberEmail(addedByEmail);
        if (requesters.isEmpty()) {
            throw new IllegalArgumentException("요청자를 찾을 수 없습니다.");
        }
        
        // 여러 레코드가 있는 경우 로깅
        if (requesters.size() > 1) {
            log.info("MemberEntity에서 동일한 이메일을 가진 {}개의 레코드를 찾았습니다. addedByEmail: {}", requesters.size(), addedByEmail);
            for (int i = 0; i < requesters.size(); i++) {
                MemberEntity requester = requesters.get(i);
                log.info("  레코드 {}: memberId={}, memberRole={}, courseId={}, memberExpired={}", 
                    i + 1, requester.getId(), requester.getMemberRole(), requester.getCourseId(), requester.getMemberExpired());
            }
        }
        
        // 가장 적절한 레코드 선택 (만료되지 않은 레코드 중 첫 번째)
        MemberEntity requester = requesters.stream()
            .filter(m -> m.getMemberExpired() == null || m.getMemberExpired().isAfter(java.time.LocalDateTime.now()))
            .findFirst()
            .orElse(requesters.get(0)); // 만료되지 않은 레코드가 없으면 첫 번째 레코드 사용
        
        // 요청자가 채팅방에 참여하고 있는지 확인
        ChatGroupEntity requesterGroup = chatGroupRepository.findByChatRoomIdAndMemberEmail(roomId, addedByEmail);
        if (requesterGroup == null) {
            throw new IllegalArgumentException("채팅방에 참여하지 않은 사용자는 다른 사용자를 추가할 수 없습니다.");
        }
        
        // 추가할 사용자가 이미 채팅방에 참여하고 있는지 확인
        ChatGroupEntity existingGroup = chatGroupRepository.findByChatRoomIdAndMemberEmail(roomId, memberToAdd.get().getMemberEmail());
        if (existingGroup != null) {
            throw new IllegalArgumentException("이미 채팅방에 참여하고 있는 사용자입니다.");
        }
        
        // ChatGroup 생성 및 저장
        ChatGroupEntity newChatGroup = ChatGroupEntity.builder()
                .id(memberToAdd.get().getMemberId()) // memberId 사용
                .chatRoomId(roomId)
                .createdAt(Instant.now())
                .enteredAt(Instant.now())
                .exitedAt(null)
                .build();
        
        chatGroupRepository.save(newChatGroup);
        
        // 채팅방 멤버 수 증가
        chatRoom.setMemberCount(chatRoom.getMemberCount() + 1);
        chatRoomRepository.save(chatRoom);
        
        // log.info("사용자 {}가 채팅방 {}에 성공적으로 추가되었습니다. (현재 멤버 수: {})", 
        //         userId, roomId, chatRoom.getMemberCount());
    }
} 