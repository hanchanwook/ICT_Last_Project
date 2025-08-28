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
public class ClassroomEquipmentDTO {
    
    private String classEquipId; // UUID
    private String classId; // 강의실 ID
    private String equipName; // 장비명
    private String equipModel; // 장비 모델
    private Integer equipNumber; // 장비 번호
    private String equipDescription; // 장비 설명 (구매처/구매링크)
    private Integer equipRent; // 대여 여부
    private Integer equipActive; // 활성화 상태
    private LocalDate createdAt; // 생성일
    private LocalDate updatedAt; // 수정일
    
    // UUID 자동 생성
    public void generateUUID() {
        if (this.classEquipId == null) {
            this.classEquipId = UUID.randomUUID().toString();
        }
    }
} 