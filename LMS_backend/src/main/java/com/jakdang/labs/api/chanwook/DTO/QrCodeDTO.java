package com.jakdang.labs.api.chanwook.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCodeDTO {
    
    private String qrCodeId; // UUID
    private String classId; // 강의실 ID
    private String courseId; // 과정 ID
    private String qrUrl; // QR코드 URL
    private int qrCodeActive; // 활성화 상태
    private LocalDate createdAt; // 생성 시간
    private LocalDate updatedAt; // 수정 시간
    
    // UUID 자동 생성
    public void generateUUID() {
        if (this.qrCodeId == null) {
            this.qrCodeId = UUID.randomUUID().toString();
        }
    }
} 