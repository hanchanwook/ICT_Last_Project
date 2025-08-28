package com.jakdang.labs.api.lnuyasha.dto; 

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문제 검색 요청 DTO
 * 프론트엔드의 검색 및 필터링 기능에 맞춰 설계
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionSearchRequestDTO {
    
    /**
     * 검색 키워드 (문제 제목, 내용, 세부과목명 등)
     */
    private String searchTerm;
    
    /**
     * 연도 필터 (예: "2024", "2023", "2022")
     */
    private String selectedYear;
    
    /**
     * 상태 필터 (예: "완료", "진행중", "예정")
     */
    private String selectedStatus;
    
    /**
     * 강사 ID (선택적)
     */
    private String memberId;
    
    /**
     * 문제 유형 (선택적)
     */
    private String questionType;
    
    /**
     * 세부과목 ID (선택적)
     */
    private String subDetailId;
} 