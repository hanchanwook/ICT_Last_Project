package com.jakdang.labs.api.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostViewResponseDTO {
    
    /**
     * 조회수가 실제로 증가했는지 여부
     */
    private boolean viewCountIncreased;
    
    /**
     * 다음 조회 가능 시간까지 남은 시간(분)
     * 0이면 바로 조회 가능
     */
    private long remainingCooldownMinutes;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    public static PostViewResponseDTO success() {
        return PostViewResponseDTO.builder()
                .viewCountIncreased(true)
                .remainingCooldownMinutes(0)
                .message("조회수가 증가했습니다.")
                .build();
    }
    
    public static PostViewResponseDTO cooldown(long remainingMinutes) {
        return PostViewResponseDTO.builder()
                .viewCountIncreased(false)
                .remainingCooldownMinutes(remainingMinutes)
                .message("쿨타임 중입니다. " + remainingMinutes + "분 후에 다시 시도해주세요.")
                .build();
    }
    
    public static PostViewResponseDTO error(String errorMessage) {
        return PostViewResponseDTO.builder()
                .viewCountIncreased(false)
                .remainingCooldownMinutes(0)
                .message(errorMessage)
                .build();
    }
} 