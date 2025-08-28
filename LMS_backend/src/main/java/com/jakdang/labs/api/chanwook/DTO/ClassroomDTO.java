package com.jakdang.labs.api.chanwook.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
            
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomDTO {
    
    private String classId; // UUID
    private String classCode; // 강의실 코드
    private int classNumber; // 강의실 번호
    private String classCapacity; // 수용 인원 (VARCHAR)
    private int classActive; // 활성화 상태
    private int classRent; // 대여료
    private String classArea; // 면적
    private String classPersonArea; // 인당 면적
    private LocalDate createdAt; // 생성일
    private LocalDate updatedAt; // 수정일
    private String classMemo; // 메모
    private String educationId; // 학원 ID
    private String educationName; // 학원명
    
    // 프론트엔드 호환을 위한 가상 필드
    public String getClassName() {
        return classCode + "-" + classNumber;
    }
    
    public String getClassLocation() {
        return "위치 정보 없음"; // 실제 위치 정보가 없으므로 기본값
    }
    
    public int getClassCapacityAsInt() {
        try {
            return Integer.parseInt(classCapacity);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
} 