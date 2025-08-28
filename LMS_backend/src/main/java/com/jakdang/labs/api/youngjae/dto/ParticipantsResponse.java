package com.jakdang.labs.api.youngjae.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantsResponse {
    
    private boolean success;
    private String error;
    private List<ParticipantDto> participants;
    private int totalCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDto {
        private String id;
        private String name;
        private String email;
        private String role;
        private boolean isOnline;
        private Instant joinedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantsData {
        private List<ParticipantDto> participants;
        private int totalCount;
    }
} 