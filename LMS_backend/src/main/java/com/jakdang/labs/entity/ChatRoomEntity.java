package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chatroom")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String chatRoomId;
    
    private String id;
    
    private String chatRoomName;
    
    private Integer memberCount;
    
    private Instant createdAt;
} 
