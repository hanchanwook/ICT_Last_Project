package com.jakdang.labs.api.youngjae.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

import com.jakdang.labs.entity.ChatRoomEntity;
import com.jakdang.labs.entity.MemberEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomResponse {
    private boolean success;
    private String error;
    private RoomDto room;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomDto {
        private String id;
        private String name;
        private String type;
        private List<ParticipantDto> participants;
        private Instant createdAt;
        
        public static RoomDto from(ChatRoomEntity chatRoom, List<MemberEntity> participants) {
            List<ParticipantDto> participantDtos = participants.stream()
                    .map(ParticipantDto::from)
                    .toList();
            
            return RoomDto.builder()
                    .id(chatRoom.getChatRoomId())
                    .name(chatRoom.getChatRoomName())
                    .type(chatRoom.getMemberCount() == 2 ? "individual" : "group")
                    .participants(participantDtos)
                    .createdAt(chatRoom.getCreatedAt())
                    .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDto {
        private String id;
        private String name;
        private String role;
        
        public static ParticipantDto from(MemberEntity user) {
            return ParticipantDto.builder()
                    .id(user.getId())
                    .name(user.getMemberName())
                    .role(user.getMemberRole())
                    .build();
        }
    }
} 
