package com.jakdang.labs.api.lnuyasha.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 과목별 세부과목 그룹 DTO
 * 프론트엔드 요구사항에 맞는 그룹화된 구조
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectGroupDTO {
    private String mainSubject;      // 메인 과목명
    private String subjectId;        // 과목 ID
    private String subjectInfo;      // 과목 정보
    private List<SubDetailDTO> subDetails; // 세부과목 목록
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubDetailDTO {
        private String subDetailName;  // 세부과목명
        private String subDetailId;    // 세부과목 ID
        private String subDetailInfo;  // 세부과목 정보
        private int subDetailActive;   // 세부과목 활성화 여부
    }
} 