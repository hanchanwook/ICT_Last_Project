package com.jakdang.labs.api.jaegyeom.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNoticeDto {
    private String title;
    private String content;
    private String imageUrl;
    private boolean isActive = true; // 기본값 true
    private String createdBy; // 작성자 (기존 정보 유지)
    private String updatedBy; // 수정자 (토큰에서 추출한 이메일)
    private String educationId; // 교육 ID
}