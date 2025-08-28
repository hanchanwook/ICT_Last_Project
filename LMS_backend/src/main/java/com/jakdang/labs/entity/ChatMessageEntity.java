package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chatmessage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String messageId;
    
    private String id;
    
    private String chatRoomId;
    
    private String content;
    
    private Instant createdAt;
    
    // 채팅방 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatRoomId", insertable = false, updatable = false)
    private ChatRoomEntity chatRoom;
} 
