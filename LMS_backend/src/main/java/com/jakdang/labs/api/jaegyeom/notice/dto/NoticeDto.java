package com.jakdang.labs.api.jaegyeom.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeDto {
    private String id;
    private String title;
    private String content;
    private String imageUrl;
    private int isActive;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    
    private String educationId;
}