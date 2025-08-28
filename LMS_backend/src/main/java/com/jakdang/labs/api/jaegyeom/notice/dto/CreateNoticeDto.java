package com.jakdang.labs.api.jaegyeom.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoticeDto {
    private String title;
    private String content;
    private String imageUrl;
    private boolean isActive = true; // 기본값 true
    private String createdBy; // 작성자 (토큰에서 추출한 이메일)
    private String updatedBy; // 수정자

    private String educationId;
}