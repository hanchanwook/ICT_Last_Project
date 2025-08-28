package com.jakdang.labs.api.youngjae.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.ChatRoomEntity;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, String> {
    
    @Query("SELECT c FROM ChatRoomEntity c WHERE c.id = :creatorId")
    List<ChatRoomEntity> findChatRoomsByCreatorId(@Param("creatorId") String creatorId);
    
    ChatRoomEntity findByChatRoomId(String chatRoomId);
    
    @Query(value = "SELECT chatRoomId, id, chatRoomName, memberCount, createdAt FROM chatroom WHERE chatRoomId IN (:chatRoomIds)", nativeQuery = true)
    List<ChatRoomEntity> findAllByChatRoomIds(@Param("chatRoomIds") List<String> chatRoomIds);
} 
