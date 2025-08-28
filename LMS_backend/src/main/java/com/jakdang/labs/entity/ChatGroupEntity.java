package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chatgroup")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroupEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String chatGroupId;
    
    private String id;
    
    private String chatRoomId;
    
    private Instant createdAt;
    
    private Instant enteredAt;
    
    private Instant exitedAt;
    
    // ?�래 ??관�??�정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatRoomId", insertable = false, updatable = false)
    private ChatRoomEntity chatRoom;
} 
