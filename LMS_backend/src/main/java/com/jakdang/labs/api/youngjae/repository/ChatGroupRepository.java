package com.jakdang.labs.api.youngjae.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.ChatGroupEntity;

import java.util.List;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroupEntity, String> {
    
    /**
     * 특정 사용자의 참여 채팅방 ID 목록 조회 (이메일 기반)
     */
    @Query(value = "SELECT cg.chatRoomId FROM chatgroup cg " +
                   "JOIN member m ON cg.id = m.memberId " +
                   "WHERE m.memberEmail = :email", nativeQuery = true)
    List<String> findChatRoomIdsByMemberEmail(@Param("email") String email);
    
    /**
     * 특정 사용자의 참여 채팅방 ID 목록 조회 (사용자 ID 기반)
     */
    @Query(value = "SELECT cg.chatRoomId FROM chatgroup cg " +
                   "JOIN member m ON cg.id = m.memberId " +
                   "WHERE m.id = :userId", nativeQuery = true)
    List<String> findChatRoomIdsByUserId(@Param("userId") String userId);
    
    /**
     * 특정 채팅방의 참여자 목록 조회
     */
    List<ChatGroupEntity> findByChatRoomId(String chatRoomId);
    
    /**
     * 특정 사용자가 특정 채팅방에 참여 중인지 확인
     */
    boolean existsByChatGroupIdAndChatRoomId(String chatGroupId, String chatRoomId);

    /**
     * 특정 사용자의 채팅방 참여 정보 조회
     */
    ChatGroupEntity findByChatGroupIdAndChatRoomId(String chatGroupId, String chatRoomId);
    
    /**
     * 특정 사용자의 채팅방 참여 정보 조회 (이메일 기반)
     */
    @Query(value = "SELECT cg.* FROM chatgroup cg " +
                   "JOIN member m ON cg.id = m.memberId " +
                   "WHERE cg.chatRoomId = :chatRoomId AND m.memberEmail = :email", nativeQuery = true)
    ChatGroupEntity findByChatRoomIdAndMemberEmail(@Param("chatRoomId") String chatRoomId, @Param("email") String email);

    /**
     * 특정 채팅방에 특정 사용자가 참여 중인지 확인 (이메일 기반)
     */
    @Query(value = "SELECT COUNT(*) FROM chatgroup cg " +
                   "JOIN member m ON cg.id = m.memberId " +
                   "WHERE cg.chatRoomId = :chatRoomId AND m.memberEmail = :email", nativeQuery = true)
    long countByChatRoomIdAndMemberEmail(@Param("chatRoomId") String chatRoomId, @Param("email") String email);
    
    /**
     * 특정 사용자의 채팅방 참여 정보 조회 (memberId 기반)
     */
    ChatGroupEntity findByIdAndChatRoomId(String memberId, String chatRoomId);
}
