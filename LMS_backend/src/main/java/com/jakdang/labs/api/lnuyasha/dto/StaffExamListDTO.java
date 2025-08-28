package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 강사용 시험 목록 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffExamListDTO {
    
    private String examId;           // 시험 ID
    private String examName;         // 시험명
    private String examDescription;  // 시험 설명
    private Integer examTime;        // 시험 시간 (분)
    private Integer totalQuestions;  // 총 문제 수
    private Integer totalScore;      // 총점
    private Integer participantCount; // 응시자 수
    private Double avgScore;         // 평균 점수
    private Double passRate;         // 합격률 (%)
    private Instant createdAt;       // 생성일시
    private Integer examActive;      // 시험 활성화 상태
} 