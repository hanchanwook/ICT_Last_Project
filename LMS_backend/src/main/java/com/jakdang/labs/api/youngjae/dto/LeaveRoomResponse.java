package com.jakdang.labs.api.youngjae.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRoomResponse {
    
    private boolean success;
    private String message;
    private String error;
    private LeaveRoomData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveRoomData {
        private String roomId;
        private Instant leftAt;
    }
} 