package com.jakdang.labs.api.youngjae.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.ChatMessageEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {
    
    /**
     * 특정 채팅방의 메시지 목록 조회 (최신순)
     */
    List<ChatMessageEntity> findByChatRoomIdOrderByCreatedAtDesc(String chatRoomId);
    
    /**
     * 특정 채팅방의 최신 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessageEntity cm WHERE cm.chatRoomId = :chatRoomId ORDER BY cm.createdAt DESC")
    List<ChatMessageEntity> findLatestMessageByChatRoomId(@Param("chatRoomId") String chatRoomId);
    
    /**
     * 특정 채팅방의 마지막 메시지 조회
     */
    Optional<ChatMessageEntity> findFirstByChatRoomIdOrderByCreatedAtDesc(String chatRoomId);
    
    /**
     * 특정 사용자가 보낸 메시지 목록 조회
     */
    List<ChatMessageEntity> findByIdOrderByCreatedAtDesc(String senderId);
    
    /**
     * 특정 채팅방의 메시지 개수 조회
     */
    long countByChatRoomId(String chatRoomId);
    
    /**
     * 특정 채팅방의 메시지 목록 조회 (최신순, 페이지네이션)
     */
    List<ChatMessageEntity> findByChatRoomIdOrderByCreatedAtDesc(String chatRoomId, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 특정 채팅방의 메시지 목록 조회 (최신순)
     */
    List<ChatMessageEntity> findByChatRoomIdOrderByCreatedAtAsc(String chatRoomId);
    
    /**
     * 특정 채팅방의 특정 시간 이후 메시지 목록 조회 (최신순)
     */
    @Query("SELECT cm FROM ChatMessageEntity cm WHERE cm.chatRoomId = :chatRoomId AND cm.createdAt >= :afterTime ORDER BY cm.createdAt ASC")
    List<ChatMessageEntity> findByChatRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(
        @Param("chatRoomId") String chatRoomId, 
        @Param("afterTime") java.time.Instant afterTime
    );
} 