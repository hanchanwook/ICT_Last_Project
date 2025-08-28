package com.jakdang.labs.api.youngjae.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastExitedAtResponse {
    private boolean success;
    private Instant lastExitedAt;
    private String message;
    private String error;
} 